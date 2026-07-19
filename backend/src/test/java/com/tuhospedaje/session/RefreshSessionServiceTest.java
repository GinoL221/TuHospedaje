package com.tuhospedaje.session;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.entity.RefreshToken;
import com.tuhospedaje.entity.RefreshTokenFamily;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.RefreshTokenFamilyRepository;
import com.tuhospedaje.repository.RefreshTokenRepository;
import com.tuhospedaje.repository.SessionSecurityEventRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.RefreshSessionService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RefreshSessionServiceTest {
    private static final Instant ISSUED_AT = Instant.parse("2030-01-01T12:00:00.123456Z");

    @Autowired private RefreshSessionService sessions;
    @Autowired private UserRepository users;
    @Autowired private RefreshTokenRepository tokens;
    @Autowired private RefreshTokenFamilyRepository families;
    @Autowired private SessionSecurityEventRepository events;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MariaDBContainer<?> mariaDb;
    @Autowired private JdbcTemplate jdbc;
    @MockBean(name = "utcClockSupplier") private Supplier<Clock> clock;

    @Test
    void issuesForExactlyThirtyDaysAndRotatesAtTheOriginalAbsoluteBoundary() {
        setClock(ISSUED_AT);
        var first = sessions.issue(user("boundary@example.test"));
        RefreshToken initial = tokensFor(first.familyId()).get(0);

        assertThat(first.absoluteExpiresAt()).isEqualTo(ISSUED_AT.plus(30, ChronoUnit.DAYS));
        assertThat(initial.getIssuedAt()).isEqualTo(ISSUED_AT);
        assertThat(initial.getExpiresAt()).isEqualTo(first.absoluteExpiresAt());

        setClock(ISSUED_AT.plus(29, ChronoUnit.DAYS));
        var rotated = sessions.rotate(first.refreshCredential());

        assertThat(rotated.absoluteExpiresAt()).isEqualTo(first.absoluteExpiresAt());
        assertThat(rotated.absoluteExpiresAt()).isEqualTo(ISSUED_AT.plus(30, ChronoUnit.DAYS));
    }

    @Test
    void persistsLineageAndRejectsTheSuccessorAfterReuseRevokesTheFamily() {
        setClock(ISSUED_AT);
        var first = sessions.issue(user("lineage@example.test"));
        setClock(ISSUED_AT.plus(1, ChronoUnit.HOURS));
        var successor = sessions.rotate(first.refreshCredential());
        List<RefreshToken> lineage = tokensFor(first.familyId());

        assertThat(lineage).hasSize(2);
        assertThat(lineage.get(0).getGeneration()).isZero();
        assertThat(lineage.get(0).getConsumedAt()).isEqualTo(ISSUED_AT.plus(1, ChronoUnit.HOURS));
        assertThat(lineage.get(1).getGeneration()).isEqualTo(1);
        assertThat(lineage.get(1).getPredecessor().getId()).isEqualTo(lineage.get(0).getId());
        assertThat(families.findById(first.familyId()).orElseThrow().getCurrentGeneration()).isEqualTo(1);

        assertThatThrownBy(() -> sessions.rotate(first.refreshCredential())).isInstanceOf(RefreshSessionService.Rejected.class);
        assertThatThrownBy(() -> sessions.rotate(successor.refreshCredential())).isInstanceOf(RefreshSessionService.Rejected.class);
        assertThat(families.findById(first.familyId()).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(events.countByFamilyId(first.familyId())).isEqualTo(1);
        assertThat(tokensFor(first.familyId())).allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
    }

    @Test
    void reuseRevokesOnlyTheReplayedFamilyAndRecordsItsDetectionTime() {
        setClock(ISSUED_AT);
        User user = user("reuse-isolation@example.test");
        var familyA = sessions.issue(user);
        var familyB = sessions.issue(user);

        setClock(ISSUED_AT.plus(1, ChronoUnit.HOURS));
        var successorA = sessions.rotate(familyA.refreshCredential());

        Instant reuseDetectedAt = ISSUED_AT.plus(2, ChronoUnit.HOURS);
        setClock(reuseDetectedAt);
        assertRejected(familyA.refreshCredential());

        setClock(reuseDetectedAt.plus(1, ChronoUnit.MINUTES));
        assertRejected(successorA.refreshCredential());

        RefreshTokenFamily revokedFamilyA = families.findById(familyA.familyId()).orElseThrow();
        assertThat(revokedFamilyA.getRevokedAt()).isEqualTo(reuseDetectedAt);
        assertThat(revokedFamilyA.getReuseDetectedAt()).isEqualTo(reuseDetectedAt);
        assertTokensTerminallyRevoked(familyA.familyId());
        assertThat(events.countByFamilyId(familyA.familyId())).isEqualTo(1);

        assertThat(families.findById(familyB.familyId()).orElseThrow().getRevokedAt()).isNull();
        assertThat(tokensFor(familyB.familyId())).allSatisfy(token -> assertThat(token.getRevokedAt()).isNull());
        assertThat(sessions.rotate(familyB.refreshCredential()).familyId()).isEqualTo(familyB.familyId());
    }

    @Test
    void revokingOneFamilyDoesNotRevokeAnotherActiveFamilyForTheSameUser() {
        setClock(ISSUED_AT);
        User user = user("multi-family@example.test");
        var first = sessions.issue(user);
        var second = sessions.issue(user);

        sessions.revokeCurrent(first.refreshCredential());

        assertThat(families.findById(first.familyId()).orElseThrow().getRevokedAt()).isNotNull();
        assertTokensTerminallyRevoked(first.familyId());
        assertThat(families.findById(second.familyId()).orElseThrow().getRevokedAt()).isNull();
        assertThat(tokensFor(second.familyId())).allSatisfy(token -> assertThat(token.getRevokedAt()).isNull());
        assertThat(sessions.rotate(second.refreshCredential()).familyId()).isEqualTo(second.familyId());
    }

    @Test
    void revokeAllTerminatesEveryActiveFamilyForTheSameUser() {
        setClock(ISSUED_AT);
        User user = user("revoke-all@example.test");
        var first = sessions.issue(user);
        var second = sessions.issue(user);
        var alreadyRevoked = sessions.issue(user);
        RefreshTokenFamily revokedFamily = families.findById(alreadyRevoked.familyId()).orElseThrow();
        revokedFamily.setRevokedAt(ISSUED_AT.minusSeconds(1));
        revokedFamily.setRevocationReason("ADMIN");
        families.saveAndFlush(revokedFamily);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        sessions.revokeAll(user.getId(), "LOGOUT_ALL");

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        assertThat(families.findById(first.familyId()).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(families.findById(second.familyId()).orElseThrow().getRevokedAt()).isNotNull();
        assertTokensTerminallyRevoked(first.familyId());
        assertTokensTerminallyRevoked(second.familyId());
        assertTokensTerminallyRevoked(alreadyRevoked.familyId());
    }

    @Test
    void rejectsAnExpiredFamilyWhenItsTokenIsStillIndependentlyUsable() {
        setClock(ISSUED_AT);
        var issued = sessions.issue(user("family-expiry@example.test"));
        RefreshTokenFamily family = families.findById(issued.familyId()).orElseThrow();
        RefreshToken token = tokensFor(issued.familyId()).get(0);
        family.setAbsoluteExpiresAt(ISSUED_AT.minusSeconds(1));
        token.setExpiresAt(ISSUED_AT.plus(1, ChronoUnit.DAYS));
        families.save(family);
        tokens.save(token);

        assertRejected(issued.refreshCredential());
        assertThat(families.findById(issued.familyId()).orElseThrow().getRevokedAt()).isNull();
        assertThat(tokensFor(issued.familyId()).get(0).getRevokedAt()).isNull();
    }

    @Test
    void rejectsMalformedUnknownExpiredRevokedDisabledAndInconsistentCredentials() {
        setClock(ISSUED_AT);
        User rejectedUser = user("rejection@example.test");
        var issued = sessions.issue(rejectedUser);
        RefreshTokenFamily family = families.findById(issued.familyId()).orElseThrow();

        assertRejected("malformed");
        assertRejected("rt1.unknown." + issued.refreshCredential().substring(issued.refreshCredential().lastIndexOf('.') + 1));

        RefreshToken token = tokensFor(issued.familyId()).get(0);
        token.setExpiresAt(ISSUED_AT.minusSeconds(1));
        tokens.save(token);
        assertRejected(issued.refreshCredential());

        token.setExpiresAt(ISSUED_AT.plus(30, ChronoUnit.DAYS));
        token.setRevokedAt(ISSUED_AT);
        tokens.save(token);
        assertRejected(issued.refreshCredential());

        token.setRevokedAt(null);
        rejectedUser.setEnabled(false);
        users.save(rejectedUser);
        tokens.save(token);
        assertRejected(issued.refreshCredential());

        rejectedUser.setEnabled(true);
        family.setCurrentGeneration(9);
        users.save(rejectedUser);
        families.save(family);
        assertRejected(issued.refreshCredential());
    }

    @Test
    void concurrentPresentationCreatesOneSuccessorThenRecordsOneReuseEvent() throws Exception {
        setClock(ISSUED_AT);
        var issued = sessions.issue(user("concurrent@example.test"));
        var executor = Executors.newFixedThreadPool(2);
        try {
            java.util.List<Future<Boolean>> results = executor.invokeAll(java.util.List.of(
                    () -> attempt(issued.refreshCredential()), () -> attempt(issued.refreshCredential())));
            long successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) successes++;
            }
            assertThat(successes).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
        assertThat(tokens.countByFamilyId(issued.familyId())).isEqualTo(2);
        assertThat(events.countByFamilyId(issued.familyId())).isEqualTo(1);
        assertThat(families.findById(issued.familyId()).orElseThrow().getRevokedAt()).isNotNull();
    }

    @Test
    void concurrentRefreshAndLogoutEndsWithTheFamilyRevoked() throws Exception {
        setClock(ISSUED_AT);
        var issued = sessions.issue(user("logout-race@example.test"));
        var executor = Executors.newFixedThreadPool(2);
        try {
            executor.invokeAll(java.util.List.of(
                    () -> attempt(issued.refreshCredential()),
                    () -> { sessions.revokeCurrent(issued.refreshCredential()); return false; }));
        } finally {
            executor.shutdownNow();
        }
        assertThat(families.findById(issued.familyId()).orElseThrow().getRevokedAt()).isNotNull();
    }

    @Test
    void mariaDb1011FamilyRowLockShowsTheContenderWaitingBeforeRelease() throws Exception {
        setClock(ISSUED_AT);
        var issued = sessions.issue(user("lock-wait@example.test"));
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        CountDownLatch contenderEnteredServicePath = new CountDownLatch(1);
        AtomicReference<Long> ownerConnectionId = new AtomicReference<>();
        AtomicReference<Long> contenderConnectionId = new AtomicReference<>();
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> lockOwner = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        families.findByIdForUpdate(issued.familyId()).orElseThrow();
                        ownerConnectionId.set(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
                        lockAcquired.countDown();
                        await(releaseLock);
                    }));
            assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Boolean> refresh = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                contenderConnectionId.set(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
                contenderEnteredServicePath.countDown();
                return attempt(issued.refreshCredential());
            }));
            assertThat(contenderEnteredServicePath.await(5, TimeUnit.SECONDS)).isTrue();
            awaitContenderMariaDb1011LockWait(contenderConnectionId.get(), ownerConnectionId.get(), issued.familyId());
            assertThat(refresh.isDone()).isFalse();

            releaseLock.countDown();
            lockOwner.get(5, TimeUnit.SECONDS);
            assertThat(refresh.get(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            releaseLock.countDown();
            executor.shutdownNow();
        }
    }

    private boolean attempt(String credential) {
        try {
            sessions.rotate(credential);
            return true;
        } catch (RefreshSessionService.Rejected expected) {
            return false;
        }
    }

    private void setClock(Instant instant) {
        when(clock.get()).thenReturn(Clock.fixed(instant, ZoneOffset.UTC));
    }

    private void assertRejected(String credential) {
        assertThatThrownBy(() -> sessions.rotate(credential)).isInstanceOf(RefreshSessionService.Rejected.class);
    }

    private List<RefreshToken> tokensFor(long familyId) {
        return tokens.findAll().stream()
                .filter(token -> token.getFamily().getId().equals(familyId))
                .sorted(java.util.Comparator.comparingLong(RefreshToken::getGeneration))
                .toList();
    }

    private void assertTokensTerminallyRevoked(long familyId) {
        List<RefreshToken> familyTokens = tokensFor(familyId);
        assertThat(familyTokens).isNotEmpty();
        assertThat(familyTokens).allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
    }

    private void awaitContenderMariaDb1011LockWait(long contenderConnectionId, long ownerConnectionId, long familyId) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        JdbcTemplate privilegedJdbc = new JdbcTemplate(
                new DriverManagerDataSource(mariaDb.getJdbcUrl(), "root", mariaDb.getPassword()));
        List<LockWaitEvidence> observed = List.of();
        while (System.nanoTime() < deadline) {
            observed = privilegedJdbc.query("""
                    SELECT waiting.trx_mysql_thread_id AS waiting_connection_id,
                           waiting.trx_state AS waiting_state,
                           waiting.trx_query AS waiting_query,
                           blocking.trx_mysql_thread_id AS blocking_connection_id,
                           blocking.trx_query AS blocking_query
                    FROM information_schema.innodb_lock_waits lock_waits
                    JOIN information_schema.innodb_trx waiting ON waiting.trx_id = lock_waits.requesting_trx_id
                    JOIN information_schema.innodb_trx blocking ON blocking.trx_id = lock_waits.blocking_trx_id
                    WHERE waiting.trx_mysql_thread_id = ?
                    """, (resultSet, rowNum) -> new LockWaitEvidence(
                    resultSet.getLong("waiting_connection_id"),
                    resultSet.getString("waiting_state"),
                    resultSet.getString("waiting_query"),
                    resultSet.getLong("blocking_connection_id"),
                    resultSet.getString("blocking_query")), contenderConnectionId);
            boolean expectedWaitObserved = observed.stream().anyMatch(wait -> wait.blockingConnectionId() == ownerConnectionId
                    && "LOCK WAIT".equals(wait.waitingState())
                    && wait.waitingQuery() != null
                    && wait.waitingQuery().toLowerCase(Locale.ROOT).contains("refresh_token_families"));
            if (expectedWaitObserved) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while correlating contender MariaDB 10.11 lock wait", exception);
            }
        }
        throw new AssertionError("MariaDB 10.11 did not expose a correlated lock wait before release: familyId=" + familyId
                + ", contenderConnectionId=" + contenderConnectionId + ", ownerConnectionId=" + ownerConnectionId
                + ", observed=" + observed);
    }

    private record LockWaitEvidence(long waitingConnectionId, String waitingState, String waitingQuery,
                                    long blockingConnectionId, String blockingQuery) {
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test lock release");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while holding test lock", exception);
        }
    }

    private User user(String email) {
        return users.save(User.builder().email(email).password("secret").firstName("Test")
                .lastName("User").role(RoleEnum.USER).enabled(true).build());
    }
}
