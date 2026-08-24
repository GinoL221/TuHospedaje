package com.tuhospedaje.email.outbox;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.EmailOutboxStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.impl.EmailOutboxScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.TestTransaction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class EmailOutboxFoundationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationContext applicationContext;

    private final List<Long> fixtureUserIds = new ArrayList<>();

    @AfterEach
    void cleanUpCreatedFixtures() {
        if (TestTransaction.isActive()) {
            return;
        }
        deleteCreatedFixtures();
    }

    @AfterTransaction
    void cleanUpCreatedFixturesAfterTransaction() {
        deleteCreatedFixtures();
    }

    private void deleteCreatedFixtures() {
        List<Long> userIds = List.copyOf(fixtureUserIds);
        fixtureUserIds.clear();
        if (userIds.isEmpty()) {
            return;
        }

        TransactionTemplate cleanup = new TransactionTemplate(transactionManager);
        cleanup.executeWithoutResult(status -> {
            String placeholders = String.join(", ", Collections.nCopies(userIds.size(), "?"));
            String userIdsClause = "(" + placeholders + ")";
            Object[] parameters = userIds.toArray();

            jdbcTemplate.update("DELETE FROM email_outbox WHERE user_id IN " + userIdsClause, parameters);
            jdbcTemplate.update("DELETE FROM users WHERE id IN " + userIdsClause, parameters);
        });
    }

    @Test
    void migrationsCreateEmailOutboxTableRetryScheduleAndUserFlag() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = 'email_outbox'
                """, Integer.class);
        Integer userFlagCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'users'
                  AND column_name = 'email_delivery_warning_pending'
                """, Integer.class);
        Integer retryColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'email_outbox'
                  AND column_name = 'next_attempt_at'
                """, Integer.class);

        assertThat(tableCount).isEqualTo(1);
        assertThat(userFlagCount).isEqualTo(1);
        assertThat(retryColumnCount).isEqualTo(1);
    }

    @Test
    void entityMapsStatusEnumAndPreRenderedFields() {
        User user = saveUser("outbox-entity@test.com");

        EmailOutbox outbox = new EmailOutbox();
        outbox.setUser(user);
        outbox.setEmailType("WELCOME");
        outbox.setAggregateId(user.getId().toString());
        outbox.setRecipient("outbox-entity@test.com");
        outbox.setSubject("Welcome");
        outbox.setHtmlBody("<p>Hello</p>");
        outbox.setStatus(EmailOutboxStatus.PENDING);
        outbox.setFailedAttempts(0);

        EmailOutbox saved = emailOutboxRepository.save(outbox);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(saved.getRecipient()).isEqualTo("outbox-entity@test.com");
    }

    @Test
    void disabledDispatchPreservesEligibleWelcomeRowsWithoutConsumingAttempts() {
        User user = saveUser("disabled-dispatch@test.com");
        EmailOutbox saved = emailOutboxRepository.save(buildOutbox(
                user, "WELCOME", "disabled-dispatch-1", "disabled-dispatch@test.com"));

        assertThat(applicationContext.getBeansOfType(EmailOutboxScheduler.class)).isEmpty();

        EmailOutbox preserved = emailOutboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(preserved.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(preserved.getFailedAttempts()).isZero();
        assertThat(preserved.getLeaseToken()).isNull();
        assertThat(preserved.getLeaseUntil()).isNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void preservedEligibleWelcomeRowCanBeClaimedWhenDispatchBecomesAvailable() {
        User user = saveUser("reenabled-dispatch@test.com");
        EmailOutbox saved = emailOutboxRepository.save(buildOutbox(
                user, "WELCOME", "reenabled-dispatch-1", "reenabled-dispatch@test.com"));

        List<EmailOutbox> claimed = emailOutboxRepository.claimEligible(Instant.now(), 1);

        assertThat(claimed).extracting(EmailOutbox::getId).containsExactly(saved.getId());
        EmailOutbox reclaimed = emailOutboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(reclaimed.getStatus()).isEqualTo(EmailOutboxStatus.PROCESSING);
        assertThat(reclaimed.getFailedAttempts()).isZero();
    }

    @Test
    void uniqueConstraintRejectsDuplicateEmailTypeAndAggregateId() {
        User user = saveUser("unique@test.com");

        EmailOutbox first = buildOutbox(user, "WELCOME", "aggregate-1", "unique@test.com");
        emailOutboxRepository.save(first);

        EmailOutbox duplicate = buildOutbox(user, "WELCOME", "aggregate-1", "unique@test.com");

        assertThatThrownBy(() -> emailOutboxRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void claimReturnsOnlyPendingRowsOrderedById() {
        User user = saveUser("claim@test.com");

        EmailOutbox pending = buildOutbox(user, "WELCOME", "claim-1", "claim@test.com");
        emailOutboxRepository.save(pending);

        EmailOutbox processing = buildOutbox(user, "WELCOME", "claim-2", "claim@test.com");
        processing.setStatus(EmailOutboxStatus.PROCESSING);
        processing.setLeaseToken(UUID.randomUUID().toString());
        processing.setLeaseUntil(Instant.now().plus(5, ChronoUnit.MINUTES));
        emailOutboxRepository.save(processing);

        List<EmailOutbox> claimed = emailOutboxRepository.claimEligible(Instant.now(), 1_000);

        assertThat(claimed).filteredOn(outbox -> outbox.getId().equals(pending.getId()))
                .singleElement().satisfies(outbox -> {
                    assertThat(outbox.getAggregateId()).isEqualTo("claim-1");
                    assertThat(outbox.getLeaseToken()).isNotNull();
                    assertThat(outbox.getStatus()).isEqualTo(EmailOutboxStatus.PROCESSING);
                });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void claimReclaimsExpiredProcessingLease() {
        User user = saveUser("expired@test.com");

        EmailOutbox expired = buildOutbox(user, "WELCOME", "expired-1", "expired@test.com");
        expired.setStatus(EmailOutboxStatus.PROCESSING);
        String oldToken = UUID.randomUUID().toString();
        expired.setLeaseToken(oldToken);
        expired.setLeaseUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
        emailOutboxRepository.save(expired);

        Instant now = Instant.now();
        Integer eligible = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM email_outbox
                WHERE id = ? AND status = 'PROCESSING' AND lease_until < ?
                """, Integer.class, expired.getId(), mariaDbDateTime(now));
        assertThat(eligible).isEqualTo(1);

        List<EmailOutbox> claimed = emailOutboxRepository.claimEligible(now, 1_000);

        EmailOutbox reclaimed = claimed.stream()
                .filter(outbox -> outbox.getId().equals(expired.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(reclaimed.getLeaseUntil()).isAfter(Instant.now());
        assertThat(reclaimed.getLeaseToken()).isNotEqualTo(oldToken);

        int staleCompletion = new TransactionTemplate(transactionManager).execute(status ->
                emailOutboxRepository.markDelivered(reclaimed.getId(), oldToken, now));
        int currentCompletion = new TransactionTemplate(transactionManager).execute(status ->
                emailOutboxRepository.markDelivered(reclaimed.getId(), reclaimed.getLeaseToken(), now));

        assertThat(staleCompletion).isZero();
        assertThat(currentCompletion).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void claimSkipsLockedRowsAndClaimsTheNextCandidate() throws Exception {
        User user = saveUser("skip-locked@test.com");
        EmailOutbox locked = buildOutbox(user, "WELCOME", "skip-locked-1", "skip-locked@test.com");
        EmailOutbox available = buildOutbox(user, "WELCOME", "skip-locked-2", "skip-locked@test.com");
        emailOutboxRepository.save(locked);
        emailOutboxRepository.save(available);

        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        Future<?> lockOwner = null;
        try {
            lockOwner = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        jdbcTemplate.queryForObject("SELECT id FROM email_outbox WHERE id = ? FOR UPDATE",
                                Long.class, locked.getId());
                        lockAcquired.countDown();
                        await(releaseLock);
                    }));
            assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            Future<List<EmailOutbox>> claim = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .execute(status -> emailOutboxRepository.claimEligible(Instant.now(), 1)));
            List<EmailOutbox> claimed = claim.get(5, TimeUnit.SECONDS);

            assertThat(claimed).extracting(EmailOutbox::getAggregateId)
                    .containsExactly("skip-locked-2");
            releaseLock.countDown();
            lockOwner.get(5, TimeUnit.SECONDS);
        } finally {
            releaseLock.countDown();
            if (lockOwner != null) {
                lockOwner.get(5, TimeUnit.SECONDS);
            }
            executor.shutdownNow();
            jdbcTemplate.update("DELETE FROM email_outbox WHERE id IN (?, ?)", locked.getId(), available.getId());
        }
    }

    @Test
    void markDeliveredRequiresMatchingToken() {
        User user = saveUser("delivered@test.com");

        String token = UUID.randomUUID().toString();
        EmailOutbox outbox = buildOutbox(user, "WELCOME", "delivered-1", "delivered@test.com");
        outbox.setStatus(EmailOutboxStatus.PROCESSING);
        outbox.setLeaseToken(token);
        outbox.setLeaseUntil(Instant.now().plus(5, ChronoUnit.MINUTES));
        EmailOutbox saved = emailOutboxRepository.save(outbox);

        int updated = emailOutboxRepository.markDelivered(saved.getId(), token, Instant.now());
        assertThat(updated).isEqualTo(1);

        EmailOutbox found = emailOutboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(EmailOutboxStatus.DELIVERED);
        assertThat(found.getCompletedAt()).isNotNull();
    }

    @Test
    void markDeliveredIgnoresStaleToken() {
        User user = saveUser("stale-delivered@test.com");

        String oldToken = UUID.randomUUID().toString();
        EmailOutbox outbox = buildOutbox(user, "WELCOME", "stale-delivered-1", "stale-delivered@test.com");
        outbox.setStatus(EmailOutboxStatus.PROCESSING);
        outbox.setLeaseToken(oldToken);
        outbox.setLeaseUntil(Instant.now().plus(5, ChronoUnit.MINUTES));
        EmailOutbox saved = emailOutboxRepository.save(outbox);

        int updated = emailOutboxRepository.markDelivered(saved.getId(), UUID.randomUUID().toString(), Instant.now());
        assertThat(updated).isZero();

        EmailOutbox found = emailOutboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(EmailOutboxStatus.PROCESSING);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleTokenCannotCompleteDeliveredRetryOrFailedOutcomes() {
        User user = saveUser("stale-outcomes@test.com");
        String currentToken = UUID.randomUUID().toString();
        EmailOutbox outbox = buildOutbox(user, "WELCOME", "stale-outcomes-1", "stale-outcomes@test.com");
        outbox.setStatus(EmailOutboxStatus.PROCESSING);
        outbox.setLeaseToken(currentToken);
        outbox.setLeaseUntil(Instant.now().plus(5, ChronoUnit.MINUTES));
        EmailOutbox saved = emailOutboxRepository.save(outbox);
        String staleToken = UUID.randomUUID().toString();

        int delivered = new TransactionTemplate(transactionManager).execute(status ->
                emailOutboxRepository.markDelivered(saved.getId(), staleToken, Instant.now()));
        int retry = new TransactionTemplate(transactionManager).execute(status ->
                emailOutboxRepository.releaseForRetry(saved.getId(), staleToken, "SMTP_UNAVAILABLE",
                        Instant.now().plus(1, ChronoUnit.MINUTES)));
        int failed = new TransactionTemplate(transactionManager).execute(status ->
                emailOutboxRepository.markFailed(saved.getId(), staleToken, Instant.now(), "SMTP_UNAVAILABLE"));

        assertThat(delivered).isZero();
        assertThat(retry).isZero();
        assertThat(failed).isZero();
        EmailOutbox found = emailOutboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(EmailOutboxStatus.PROCESSING);
        assertThat(found.getLeaseToken()).isEqualTo(currentToken);
        assertThat(found.getFailedAttempts()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void markDeliveredRejectsCompletionAfterTheLeaseExpired() {
        User user = saveUser("expired-delivery@test.com");
        String token = UUID.randomUUID().toString();
        Instant leaseUntil = Instant.parse("2026-08-24T15:05:00Z");
        EmailOutbox outbox = buildOutbox(user, "WELCOME", "expired-delivery-1", "expired-delivery@test.com");
        outbox.setStatus(EmailOutboxStatus.PROCESSING);
        outbox.setLeaseToken(token);
        outbox.setLeaseUntil(leaseUntil);
        EmailOutbox saved = emailOutboxRepository.save(outbox);

        int updated = new TransactionTemplate(transactionManager).execute(status ->
                emailOutboxRepository.markDelivered(saved.getId(), token, leaseUntil.plusSeconds(1)));

        assertThat(updated).isZero();
        EmailOutbox found = emailOutboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(EmailOutboxStatus.PROCESSING);
        assertThat(found.getLeaseToken()).isEqualTo(token);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void releaseForRetryPersistsNextAttemptAndWaitsUntilItIsDue() {
        User user = saveUser("retry-schedule@test.com");
        String token = UUID.randomUUID().toString();
        EmailOutbox outbox = buildOutbox(user, "WELCOME", "retry-schedule-1", "retry-schedule@test.com");
        outbox.setStatus(EmailOutboxStatus.PROCESSING);
        outbox.setLeaseToken(token);
        outbox.setLeaseUntil(Instant.now().plus(5, ChronoUnit.MINUTES));
        EmailOutbox saved = emailOutboxRepository.save(outbox);
        Instant now = Instant.now();
        Instant nextAttemptAt = now.plus(10, ChronoUnit.MINUTES);

        int updated = new TransactionTemplate(transactionManager).execute(status ->
                emailOutboxRepository.releaseForRetry(
                        saved.getId(), token, "SMTP_UNAVAILABLE", nextAttemptAt));

        assertThat(updated).isEqualTo(1);
        EmailOutbox released = emailOutboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(released.getNextAttemptAt())
                .isEqualTo(nextAttemptAt.truncatedTo(ChronoUnit.MICROS));
        assertThat(emailOutboxRepository.claimEligible(now, 1_000))
                .filteredOn(candidate -> candidate.getId().equals(saved.getId())).isEmpty();
        assertThat(emailOutboxRepository.claimEligible(nextAttemptAt.plusSeconds(1), 1_000))
                .extracting(EmailOutbox::getId).containsExactly(saved.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void terminalFailureAtMaximumAttemptsIsNotClaimedAgain() {
        User user = saveUser("attempt-exhaustion@test.com");
        String token = UUID.randomUUID().toString();
        EmailOutbox outbox = buildOutbox(user, "WELCOME", "attempt-exhaustion-1", "attempt-exhaustion@test.com");
        outbox.setStatus(EmailOutboxStatus.PROCESSING);
        outbox.setFailedAttempts(2);
        outbox.setLeaseToken(token);
        outbox.setLeaseUntil(Instant.now().plus(5, ChronoUnit.MINUTES));
        EmailOutbox saved = emailOutboxRepository.save(outbox);
        Instant outcomeTime = Instant.now();

        int updated = new TransactionTemplate(transactionManager).execute(status ->
                emailOutboxRepository.markFailed(saved.getId(), token, outcomeTime, "SMTP_UNAVAILABLE"));

        assertThat(updated).isEqualTo(1);
        EmailOutbox failed = emailOutboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
        assertThat(failed.getFailedAttempts()).isEqualTo(3);
        assertThat(emailOutboxRepository.claimEligible(outcomeTime.plus(1, ChronoUnit.HOURS), 1))
                .extracting(EmailOutbox::getId)
                .doesNotContain(saved.getId());
    }

    @Test
    void purgeWelcomeCompletedBeforeRetainsActiveAndNonWelcomeRecords() {
        User user = saveUser("purge@test.com");

        EmailOutbox delivered = buildOutbox(user, "WELCOME", "purge-delivered", "purge@test.com");
        delivered.setStatus(EmailOutboxStatus.DELIVERED);
        delivered.setCompletedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        emailOutboxRepository.save(delivered);

        EmailOutbox failed = buildOutbox(user, "WELCOME", "purge-failed", "purge@test.com");
        failed.setStatus(EmailOutboxStatus.FAILED);
        failed.setCompletedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        emailOutboxRepository.save(failed);

        EmailOutbox pending = buildOutbox(user, "WELCOME", "purge-pending", "purge@test.com");
        emailOutboxRepository.save(pending);

        EmailOutbox reservation = buildOutbox(user, "RESERVATION_CONFIRMATION", "purge-reservation", "purge@test.com");
        reservation.setStatus(EmailOutboxStatus.DELIVERED);
        reservation.setCompletedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        emailOutboxRepository.save(reservation);

        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        long expectedDeleted = emailOutboxRepository.findAll().stream()
                .filter(outbox -> (outbox.getStatus() == EmailOutboxStatus.DELIVERED
                        || outbox.getStatus() == EmailOutboxStatus.FAILED)
                        && outbox.getEmailType().equals("WELCOME")
                        && outbox.getCompletedAt() != null
                        && outbox.getCompletedAt().isBefore(cutoff))
                .count();
        int deleted = emailOutboxRepository.purgeWelcomeCompletedBefore(cutoff);
        assertThat(deleted).isEqualTo(expectedDeleted);
        assertThat(emailOutboxRepository.findById(delivered.getId())).isEmpty();
        assertThat(emailOutboxRepository.findById(failed.getId())).isEmpty();
        assertThat(emailOutboxRepository.findById(pending.getId())).isPresent();
        assertThat(emailOutboxRepository.findById(reservation.getId())).isPresent();
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private Timestamp mariaDbDateTime(Instant instant) {
        return Timestamp.valueOf(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    private User saveUser(String email) {
        User user = userRepository.save(User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password("irrelevant")
                .role(RoleEnum.USER)
                .build());
        fixtureUserIds.add(user.getId());
        return user;
    }

    private EmailOutbox buildOutbox(User user, String emailType, String aggregateId, String recipient) {
        EmailOutbox outbox = new EmailOutbox();
        outbox.setUser(user);
        outbox.setEmailType(emailType);
        outbox.setAggregateId(aggregateId);
        outbox.setRecipient(recipient);
        outbox.setSubject("Welcome");
        outbox.setHtmlBody("<p>Hello</p>");
        outbox.setStatus(EmailOutboxStatus.PENDING);
        outbox.setFailedAttempts(0);
        return outbox;
    }
}
