package com.tuhospedaje.email.outbox;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.EmailOutboxStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

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

    @Test
    void v4MigrationCreatesEmailOutboxTableAndUserFlag() {
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

        assertThat(tableCount).isEqualTo(1);
        assertThat(userFlagCount).isEqualTo(1);
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

        List<EmailOutbox> claimed = emailOutboxRepository.claimEligible(Instant.now(), 10);

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).getAggregateId()).isEqualTo("claim-1");
        assertThat(claimed.get(0).getLeaseToken()).isNotNull();
        assertThat(claimed.get(0).getStatus()).isEqualTo(EmailOutboxStatus.PROCESSING);
    }

    @Test
    void claimReclaimsExpiredProcessingLease() {
        User user = saveUser("expired@test.com");

        EmailOutbox expired = buildOutbox(user, "WELCOME", "expired-1", "expired@test.com");
        expired.setStatus(EmailOutboxStatus.PROCESSING);
        expired.setLeaseToken(UUID.randomUUID().toString());
        expired.setLeaseUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
        emailOutboxRepository.save(expired);

        List<EmailOutbox> claimed = emailOutboxRepository.claimEligible(Instant.now(), 10);

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).getLeaseUntil()).isAfter(Instant.now());
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
    void purgeCompletedBeforeRemovesOnlyCompletedRecords() {
        User user = saveUser("purge@test.com");

        EmailOutbox delivered = buildOutbox(user, "WELCOME", "purge-delivered", "purge@test.com");
        delivered.setStatus(EmailOutboxStatus.DELIVERED);
        delivered.setCompletedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        emailOutboxRepository.save(delivered);

        EmailOutbox pending = buildOutbox(user, "WELCOME", "purge-pending", "purge@test.com");
        emailOutboxRepository.save(pending);

        int deleted = emailOutboxRepository.purgeCompletedBefore(Instant.now().minus(30, ChronoUnit.DAYS));
        assertThat(deleted).isEqualTo(1);
        assertThat(emailOutboxRepository.findAll()).hasSize(1);
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password("irrelevant")
                .role(RoleEnum.USER)
                .build());
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
