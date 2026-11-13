package com.tuhospedaje.email.outbox;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.EmailOutboxService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the order-dependent FK cleanup failure (issue #460).
 *
 * <p>Invariant: a committed {@code email_outbox} row for a user must not break the
 * cleanup a later test performs. {@code EmailOutboxServiceImpl.enqueueWelcome} writes
 * the welcome row with plain {@code REQUIRED} propagation, so the row commits whenever
 * the surrounding transaction commits — which happens in plain {@code @SpringBootTest}
 * classes with no test transaction (the polluter in the real suite is
 * {@code WelcomeResendHttpIntegrationTest}). Any later class that calls
 * {@code userRepository.deleteAll()} collides with such a row
 * ({@code FK_email_outbox_user}).
 *
 * <p>Method order is explicit because the pair only reproduces the failure when the
 * committed outbox row exists before the cleanup method runs. Method 1 commits a user
 * plus its outbox row for real, in one explicit {@code REQUIRES_NEW} transaction:
 * {@code enqueueWelcome} uses {@code REQUIRED}, so it joins that transaction and both
 * rows commit together — exactly the state a non-transactional test class leaves
 * behind. Method 2 exercises the shared cleanup inherited from
 * {@link AbstractIntegrationTest} and asserts {@code userRepository.deleteAll()}
 * succeeds.
 *
 * <p>Without the shared {@code email_outbox} cleanup in {@code AbstractIntegrationTest},
 * method 2 fails: the committed outbox row still references the user and MySQL rejects
 * the delete. With it, the row is gone before the delete and both methods pass.
 */
@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmailOutboxFkCleanupRegressionIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "outbox-fk-regression@test.com";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailOutboxService emailOutboxService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Order(1)
    void commitsAUserAndItsWelcomeOutboxRowOutsideAnyRollingBackTransaction() {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        User user = requiresNew.execute(status -> userRepository.save(User.builder()
                .firstName("Outbox")
                .lastName("Regression")
                .email(EMAIL)
                .password("hash")
                .role(RoleEnum.USER)
                .build()));

        emailOutboxService.enqueueWelcome(user, new RegisterRequest(
                "Outbox", "Regression", EMAIL, "Secret123"));

        // Both rows must be visible from outside any transaction: they are committed,
        // exactly like the rows a previous test class leaves behind in the real suite.
        assertThat(countUsers()).isEqualTo(1);
        assertThat(countOutboxRows()).isEqualTo(1);
    }

    @Test
    @Order(2)
    void sharedCleanupRemovesTheCommittedOutboxRowBeforeUsersAreDeleted() {
        // This is the exact call the previously failing classes make in their own
        // @BeforeEach. Without the inherited email_outbox cleanup, MySQL rejects it
        // with FK_email_outbox_user because the committed row still references the
        // user created by the previous method.
        userRepository.deleteAll();

        assertThat(countUsers()).isZero();
        assertThat(countOutboxRows())
                .as("email_outbox must be empty after the shared cleanup")
                .isZero();
    }

    private long countUsers() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?", Long.class, EMAIL);
        return count == null ? 0 : count;
    }

    private long countOutboxRows() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_outbox WHERE recipient = ?", Long.class, EMAIL);
        return count == null ? 0 : count;
    }
}
