package com.tuhospedaje.session;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import com.tuhospedaje.service.impl.RefreshSessionServiceImpl;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.session.refresh.enabled=true")
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
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean(name = "utcClockSupplier") private Supplier<Clock> clock;
    private ListAppender<ILoggingEvent> serviceLogs;

    @BeforeEach
    void captureServiceLogs() {
        serviceLogs = new ListAppender<>();
        serviceLogs.start();
        ((Logger) LoggerFactory.getLogger(RefreshSessionServiceImpl.class)).addAppender(serviceLogs);
    }

    @AfterEach
    void stopCapturingServiceLogs() {
        ((Logger) LoggerFactory.getLogger(RefreshSessionServiceImpl.class)).detachAppender(serviceLogs);
        serviceLogs.stop();
    }

    // Session.userId() (Design ADR-2, PR1/WU2): rotate()/revokeAll() need the owning
    // user's id to mint a new access JWT without an extra lookup by familyId. Additive
    // 4th Session component — asserted across issue, ordinary rotate, AND the retry
    // branch (the three call sites in RefreshSessionServiceImpl), since each constructs
    // its own Session instance.
    @Test
    void issueRotateAndRetryAllExposeTheOwningUserId() {
        setClock(ISSUED_AT);
        User owner = user("owner-id@example.test");
        var issued = sessions.issue(owner);
        assertThat(issued.userId()).isEqualTo(owner.getId());

        setClock(ISSUED_AT.plusSeconds(10));
        var rotated = sessions.rotate(issued.refreshCredential());
        assertThat(rotated.userId()).isEqualTo(owner.getId());

        setClock(ISSUED_AT.plusSeconds(11));
        var retried = sessions.rotate(issued.refreshCredential());
        assertThat(retried.userId()).isEqualTo(owner.getId());
    }

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

        setClock(ISSUED_AT.plus(1, ChronoUnit.HOURS).plusSeconds(5));
        assertThatThrownBy(() -> sessions.rotate(first.refreshCredential())).isInstanceOf(RefreshSessionService.Rejected.class);
        assertThatThrownBy(() -> sessions.rotate(successor.refreshCredential())).isInstanceOf(RefreshSessionService.Rejected.class);
        assertThat(families.findById(first.familyId()).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(events.countByFamilyId(first.familyId())).isEqualTo(1);
        assertThat(tokensFor(first.familyId())).allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
    }

    @Test
    void retriesTheDirectPredecessorBeforeGraceEndsWithoutChangingPersistedState() {
        setClock(ISSUED_AT);
        var first = sessions.issue(user("retry@example.test"));
        Instant consumedAt = ISSUED_AT.plusSeconds(10);
        setClock(consumedAt);
        var successor = sessions.rotate(first.refreshCredential());
        List<RefreshToken> afterRotation = tokensFor(first.familyId());
        Instant familyLastRotatedAt = families.findById(first.familyId()).orElseThrow().getLastRotatedAt();
        serviceLogs.list.clear();

        setClock(consumedAt.plusSeconds(4).plusMillis(999));
        var retry = sessions.rotate(first.refreshCredential());

        assertThat(retry.refreshCredential()).isEqualTo(successor.refreshCredential());
        assertThat(tokensFor(first.familyId())).hasSize(2);
        assertThat(families.findById(first.familyId()).orElseThrow()).satisfies(family -> {
            assertThat(family.getCurrentGeneration()).isEqualTo(1);
            assertThat(family.getLastRotatedAt()).isEqualTo(familyLastRotatedAt);
            assertThat(family.getRevokedAt()).isNull();
        });
        assertThat(tokensFor(first.familyId()).get(0).getConsumedAt()).isEqualTo(afterRotation.get(0).getConsumedAt());
        assertThat(events.countByFamilyId(first.familyId())).isZero();
        assertThat(serviceLogs.list).isEmpty();
    }

    @Test
    void exactGraceBoundaryIsReuseAndRevokesTheFamily() {
        setClock(ISSUED_AT);
        var first = sessions.issue(user("retry-boundary@example.test"));
        sessions.rotate(first.refreshCredential());

        setClock(ISSUED_AT.plusSeconds(5));
        assertRejected(first.refreshCredential());

        assertThat(families.findById(first.familyId()).orElseThrow().getRevocationReason()).isEqualTo("REUSE");
        assertThat(events.countByFamilyId(first.familyId())).isEqualTo(1);
    }

    @Test
    void advancedAncestorInsideGraceIsReuse() {
        setClock(ISSUED_AT);
        var first = sessions.issue(user("advanced-ancestor@example.test"));
        var second = sessions.rotate(first.refreshCredential());
        setClock(ISSUED_AT.plusSeconds(1));
        sessions.rotate(second.refreshCredential());

        setClock(ISSUED_AT.plusSeconds(2));
        assertRejected(first.refreshCredential());

        assertThat(families.findById(first.familyId()).orElseThrow().getRevocationReason()).isEqualTo("REUSE");
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
        assertThat(jdbc.queryForObject("SELECT revocation_reason FROM refresh_token_families WHERE id = ?",
                String.class, familyA.familyId())).isEqualTo("REUSE");
        assertThat(jdbc.queryForMap("SELECT event_type, delivery_state FROM session_security_events WHERE family_id = ?",
                familyA.familyId())).containsEntry("event_type", "REFRESH_REUSE").containsEntry("delivery_state", "PENDING");

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
    void revokingWithAConsumedTokenRejectsAndPersistsReuseSecurityState() {
        setClock(ISSUED_AT);
        var first = sessions.issue(user("logout-reuse@example.test"));
        sessions.rotate(first.refreshCredential());

        Instant reuseDetectedAt = ISSUED_AT.plus(1, ChronoUnit.HOURS);
        setClock(reuseDetectedAt);
        assertThatThrownBy(() -> sessions.revokeCurrent(first.refreshCredential()))
                .isInstanceOf(RefreshSessionService.Rejected.class);

        RefreshTokenFamily family = families.findById(first.familyId()).orElseThrow();
        assertThat(family.getRevokedAt()).isEqualTo(reuseDetectedAt);
        assertThat(family.getReuseDetectedAt()).isEqualTo(reuseDetectedAt);
        assertThat(family.getRevocationReason()).isEqualTo("REUSE");
        assertTokensTerminallyRevoked(first.familyId());
        assertThat(events.countByFamilyId(first.familyId())).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT event_type, delivery_state FROM session_security_events WHERE family_id = ?",
                first.familyId())).containsEntry("event_type", "REFRESH_REUSE").containsEntry("delivery_state", "PENDING");
    }

    @Test
    void logsPersistedRefreshReuseOnceWithoutCredentialOrUserPii() {
        setClock(ISSUED_AT);
        User user = user("reuse-log-secret@example.test");
        var first = sessions.issue(user);
        sessions.rotate(first.refreshCredential());
        setClock(ISSUED_AT.plusSeconds(5));
        serviceLogs.list.clear();

        assertThatThrownBy(() -> sessions.revokeCurrent(first.refreshCredential()))
                .isInstanceOf(RefreshSessionService.Rejected.class);
        assertThatThrownBy(() -> sessions.revokeCurrent(first.refreshCredential()))
                .isInstanceOf(RefreshSessionService.Rejected.class);

        assertThat(serviceLogs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).isEqualTo(
                    "event=refresh_session.reuse_detected family_id=" + first.familyId()
                            + " user_id=" + user.getId() + " delivery_state=PENDING");
            assertThat(event.getFormattedMessage())
                    .doesNotContain(first.refreshCredential(), "reuse-log-secret@example.test", "secret");
        });
    }

    @Test
    void revokingWithAFreshTokenPerformsLogoutWithoutReuseEvent() {
        setClock(ISSUED_AT);
        var issued = sessions.issue(user("ordinary-logout@example.test"));

        sessions.revokeCurrent(issued.refreshCredential());

        RefreshTokenFamily family = families.findById(issued.familyId()).orElseThrow();
        assertThat(family.getRevocationReason()).isEqualTo("LOGOUT");
        assertThat(family.getReuseDetectedAt()).isNull();
        assertTokensTerminallyRevoked(issued.familyId());
        assertThat(events.countByFamilyId(issued.familyId())).isZero();
    }

    @Test
    void logsOrdinaryFamilyLogoutAndDistinguishesARevocationNoOp() {
        setClock(ISSUED_AT);
        User user = user("logout-log-secret@example.test");
        var issued = sessions.issue(user);
        serviceLogs.list.clear();

        sessions.revokeCurrent(issued.refreshCredential());
        sessions.revokeCurrent(issued.refreshCredential());

        assertThat(serviceLogs.list).extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.INFO, Level.INFO);
        assertThat(serviceLogs.list).extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(
                        "event=refresh_session.family_revoked family_id=" + issued.familyId()
                                + " user_id=" + user.getId() + " reason=LOGOUT revoked=true",
                        "event=refresh_session.family_revoked family_id=" + issued.familyId()
                                + " user_id=" + user.getId() + " reason=LOGOUT revoked=false");
        assertThat(serviceLogs.list).allSatisfy(event -> assertThat(event.getFormattedMessage())
                .doesNotContain(issued.refreshCredential(), "logout-log-secret@example.test", "secret"));
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
    void logsMassRevocationCountsWithoutCopyingAnUnsafeReason() {
        setClock(ISSUED_AT);
        User user = user("mass-log-secret@example.test");
        sessions.issue(user);
        sessions.issue(user);
        serviceLogs.list.clear();

        sessions.revokeAll(user.getId(), "admin-secret@example.test");

        assertThat(serviceLogs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage()).isEqualTo(
                    "event=refresh_session.mass_revoked user_id=" + user.getId()
                            + " reason=OTHER active_tokens_revoked=2 active_families_revoked=2");
            assertThat(event.getFormattedMessage())
                    .doesNotContain("admin-secret@example.test", "mass-log-secret@example.test", "secret");
        });
    }

    // ADR-7 (Design, issue #55): revokeAll bypasses revokeFamily's per-family REUSE event
    // path entirely (bulk @Modifying queries), so admin-disable/password-change must gain
    // their own event emission — one user-scoped event (family=null), not one per family.
    @Test
    void revokeAllForAdminReasonPersistsExactlyOneSessionSecurityEvent() {
        setClock(ISSUED_AT);
        User user = user("admin-disable-event@example.test");
        sessions.issue(user);
        sessions.issue(user);

        sessions.revokeAll(user.getId(), "ADMIN");

        List<com.tuhospedaje.entity.SessionSecurityEvent> userEvents = events.findAll().stream()
                .filter(event -> event.getUser().getId().equals(user.getId()))
                .toList();
        assertThat(userEvents).singleElement().satisfies(event -> {
            assertThat(event.getFamily()).isNull();
            assertThat(event.getEventType()).isEqualTo(com.tuhospedaje.entity.SessionSecurityEvent.Type.ADMIN_DISABLE);
            assertThat(event.getDeliveryState()).isEqualTo(com.tuhospedaje.entity.SessionSecurityEvent.DeliveryState.PENDING);
            assertThat(event.getOccurredAt()).isEqualTo(ISSUED_AT);
        });
    }

    @Test
    void revokeAllForPasswordChangeReasonPersistsExactlyOneSessionSecurityEvent() {
        setClock(ISSUED_AT);
        User user = user("password-change-event@example.test");
        sessions.issue(user);
        sessions.issue(user);

        sessions.revokeAll(user.getId(), "PASSWORD_CHANGE");

        List<com.tuhospedaje.entity.SessionSecurityEvent> userEvents = events.findAll().stream()
                .filter(event -> event.getUser().getId().equals(user.getId()))
                .toList();
        assertThat(userEvents).singleElement().satisfies(event -> {
            assertThat(event.getFamily()).isNull();
            assertThat(event.getEventType()).isEqualTo(com.tuhospedaje.entity.SessionSecurityEvent.Type.PASSWORD_CHANGE);
            assertThat(event.getDeliveryState()).isEqualTo(com.tuhospedaje.entity.SessionSecurityEvent.DeliveryState.PENDING);
        });
    }

    @Test
    void revokeAllDoesNotPersistAnEventWhenNoActiveFamilyWasRevoked() {
        setClock(ISSUED_AT);
        User user = user("no-active-family@example.test");

        sessions.revokeAll(user.getId(), "ADMIN");

        assertThat(events.findAll().stream().filter(event -> event.getUser().getId().equals(user.getId()))).isEmpty();
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
    void concurrentOriginalAndRetryReturnTheSameSuccessorWithoutAdvancingAgain() throws Exception {
        setClock(ISSUED_AT);
        var issued = sessions.issue(user("concurrent@example.test"));
        var executor = Executors.newFixedThreadPool(2);
        try {
            java.util.List<Future<String>> results = executor.invokeAll(java.util.List.of(
                    () -> sessions.rotate(issued.refreshCredential()).refreshCredential(),
                    () -> sessions.rotate(issued.refreshCredential()).refreshCredential()));
            assertThat(results.get(0).get()).isEqualTo(results.get(1).get());
        } finally {
            executor.shutdownNow();
        }
        assertThat(tokens.countByFamilyId(issued.familyId())).isEqualTo(2);
        assertThat(events.countByFamilyId(issued.familyId())).isZero();
        assertThat(families.findById(issued.familyId()).orElseThrow().getRevokedAt()).isNull();
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
    void familyRowLockBlocksTheContenderUntilRelease() throws Exception {
        setClock(ISSUED_AT);
        var issued = sessions.issue(user("lock-wait@example.test"));
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        CountDownLatch contenderEnteredServicePath = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> lockOwner = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        families.findByIdForUpdate(issued.familyId()).orElseThrow();
                        lockAcquired.countDown();
                        await(releaseLock);
                    }));
            assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Boolean> refresh = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                contenderEnteredServicePath.countDown();
                return attempt(issued.refreshCredential());
            }));
            assertThat(contenderEnteredServicePath.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> refresh.get(250, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

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
