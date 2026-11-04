package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.controller.AuthController;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.Advised;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WelcomeResendHttpIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;
    @Autowired private EmailOutboxRepository outboxRepository;
    @Autowired private UserRepository users;
    @Autowired private AuthController authController;

    @Test
    @WithAnonymousUser
    void resendMethodRejectsAnonymousInvocation() {
        assertThatThrownBy(() -> authController.resendWelcome(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void failedCooldownWriteRollsBackOutboxAndTimestamp() throws Exception {
        String email = "resend-rollback@example.test";
        MvcResult registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"Valid123\"}"))
                .andExpect(status().isCreated()).andReturn();
        Cookie access = registration.getResponse().getCookie("ACCESS_TOKEN");
        Cookie csrf = registration.getResponse().getCookie("XSRF-TOKEN");
        assertThat(access).isNotNull();
        assertThat(csrf).isNotNull();
        jdbc.update("DELETE FROM email_outbox WHERE email_type = 'WELCOME' AND user_id = (SELECT id FROM users WHERE email = ?)", email);
        MethodInterceptor failAfterUpdate = invocation -> {
            Object result = invocation.proceed();
            if (invocation.getMethod().getName().equals("updateWelcomeResendRequestedAt")) {
                throw new IllegalStateException("injected failure after cooldown update");
            }
            return result;
        };
        Advised proxy = (Advised) users;
        proxy.addAdvice(0, failAfterUpdate);
        try {
            mvc.perform(post("/api/auth/welcome-email/resend").cookie(access, csrf)
                    .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isInternalServerError());
        } finally {
            proxy.removeAdvice(failAfterUpdate);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM email_outbox WHERE email_type = 'WELCOME' AND user_id = (SELECT id FROM users WHERE email = ?)", Integer.class, email)).isZero();
        assertThat(jdbc.queryForObject("SELECT welcome_resend_requested_at FROM users WHERE email = ?", java.sql.Timestamp.class, email)).isNull();
    }

    @Test
    void cooldownSurvivesCleanupOfResentMessage() throws Exception {
        String email = "resend-retention-cooldown@example.test";
        MvcResult registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"Valid123\"}"))
                .andExpect(status().isCreated()).andReturn();
        Cookie access = registration.getResponse().getCookie("ACCESS_TOKEN");
        Cookie csrf = registration.getResponse().getCookie("XSRF-TOKEN");
        assertThat(access).isNotNull();
        assertThat(csrf).isNotNull();
        jdbc.update("DELETE FROM email_outbox WHERE email_type = 'WELCOME' AND user_id = (SELECT id FROM users WHERE email = ?)", email);
        mvc.perform(post("/api/auth/welcome-email/resend").cookie(access, csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isAccepted());
        jdbc.update("UPDATE email_outbox SET status = 'DELIVERED', completed_at = '2020-01-01 00:00:00' WHERE email_type = 'WELCOME' AND user_id = (SELECT id FROM users WHERE email = ?)", email);
        Integer deleted = transactions.execute(status -> outboxRepository.purgeWelcomeCompletedBefore(Instant.parse("2021-01-01T00:00:00Z")));
        assertThat(deleted).isEqualTo(1);
        mvc.perform(post("/api/auth/welcome-email/resend").cookie(access, csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isTooManyRequests())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Retry-After")).isEqualTo("300"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM email_outbox WHERE email_type = 'WELCOME' AND user_id = (SELECT id FROM users WHERE email = ?)", Integer.class, email)).isZero();
        assertThat(jdbc.queryForObject("SELECT welcome_resend_requested_at FROM users WHERE email = ?", java.sql.Timestamp.class, email)).isNotNull();
    }

    @Test
    void staleUserUpdateMustNotEraseResendCooldown() throws Exception {
        String email = "resend-stale-user@example.test";
        MvcResult registration = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"Valid123\"}"))
                .andExpect(status().isCreated()).andReturn();
        Cookie access = registration.getResponse().getCookie("ACCESS_TOKEN");
        Cookie csrf = registration.getResponse().getCookie("XSRF-TOKEN");
        assertThat(access).isNotNull();
        assertThat(csrf).isNotNull();
        jdbc.update("DELETE FROM email_outbox WHERE email_type = 'WELCOME' AND user_id = (SELECT id FROM users WHERE email = ?)", email);

        CountDownLatch userRead = new CountDownLatch(1);
        CountDownLatch resendFinished = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var staleUpdate = executor.submit(() -> transactions.executeWithoutResult(status -> {
                var user = users.findByEmail(email).orElseThrow();
                userRead.countDown();
                try {
                    if (!resendFinished.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("resend timed out");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                user.setFirstName("Updated");
                users.saveAndFlush(user);
            }));
            assertThat(userRead.await(10, TimeUnit.SECONDS)).isTrue();
            mvc.perform(post("/api/auth/welcome-email/resend").cookie(access, csrf)
                    .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isAccepted());
            resendFinished.countDown();
            staleUpdate.get(20, TimeUnit.SECONDS);
            var secondResponse = mvc.perform(post("/api/auth/welcome-email/resend").cookie(access, csrf)
                    .header("X-XSRF-TOKEN", csrf.getValue())).andReturn().getResponse();
            assertThat(jdbc.queryForObject("SELECT welcome_resend_requested_at FROM users WHERE email = ?",
                    java.sql.Timestamp.class, email)).isNotNull();
            assertThat(secondResponse.getStatus()).isEqualTo(429);
        } finally {
            resendFinished.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void cleanupBetweenLookupAndRequeueDoesNotCauseFalseCooldown() throws Exception {
        String email = "resend-cleanup-race@example.test";
        MvcResult registration = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"Valid123\"}"))
                .andExpect(status().isCreated()).andReturn();
        Cookie access = registration.getResponse().getCookie("ACCESS_TOKEN");
        Cookie csrf = registration.getResponse().getCookie("XSRF-TOKEN");
        assertThat(access).isNotNull();
        assertThat(csrf).isNotNull();
        jdbc.update("UPDATE email_outbox SET status = 'DELIVERED', completed_at = '2020-01-01 00:00:00' WHERE email_type = 'WELCOME' AND user_id = (SELECT id FROM users WHERE email = ?)", email);

        CountDownLatch beforeRequeue = new CountDownLatch(1);
        CountDownLatch cleanupFinished = new CountDownLatch(1);
        MethodInterceptor pauseRequeue = invocation -> {
            if (invocation.getMethod().getName().equals("requeueWelcomeIfTerminalAndCooled")) {
                beforeRequeue.countDown();
                if (!cleanupFinished.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("cleanup timed out");
            }
            return invocation.proceed();
        };
        Advised proxy = (Advised) outboxRepository;
        proxy.addAdvice(0, pauseRequeue);

        var executor = Executors.newSingleThreadExecutor();
        try {
            var response = executor.submit(() -> mvc.perform(post("/api/auth/welcome-email/resend")
                    .cookie(access, csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                    .andReturn().getResponse());
            assertThat(beforeRequeue.await(10, TimeUnit.SECONDS)).isTrue();
            Integer deleted = transactions.execute(status -> outboxRepository.purgeWelcomeCompletedBefore(Instant.parse("2021-01-01T00:00:00Z")));
            assertThat(deleted).isEqualTo(1);
            cleanupFinished.countDown();
            var result = response.get(20, TimeUnit.SECONDS);
            assertThat(result.getStatus()).isEqualTo(202);
            assertThat(result.getContentAsString()).isEmpty();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM email_outbox WHERE email_type = 'WELCOME' AND status = 'PENDING' AND user_id = (SELECT id FROM users WHERE email = ?)", Integer.class, email)).isEqualTo(1);
        } finally {
            cleanupFinished.countDown();
            executor.shutdownNow();
            proxy.removeAdvice(pauseRequeue);
        }
    }

    @Test
    void reportsAnAlreadyPendingWelcomeSeparatelyFromCooldown() throws Exception {
        String email = "resend-already-pending@example.test";
        MvcResult registration = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"Valid123\"}"))
                .andExpect(status().isCreated()).andReturn();
        Cookie access = registration.getResponse().getCookie("ACCESS_TOKEN");
        Cookie csrf = registration.getResponse().getCookie("XSRF-TOKEN");
        assertThat(access).isNotNull();
        assertThat(csrf).isNotNull();
        mvc.perform(post("/api/auth/welcome-email/resend").cookie(access, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .isEqualTo("{\"status\":\"pending\"}"));
    }

    @Test
    void simultaneousRequestsDoNotBothScheduleAResend() throws Exception {
        String email = "resend-concurrent@example.test";
        MvcResult registration = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"Valid123\"}"))
                .andExpect(status().isCreated()).andReturn();
        Cookie access = registration.getResponse().getCookie("ACCESS_TOKEN");
        Cookie csrf = registration.getResponse().getCookie("XSRF-TOKEN");
        assertThat(access).isNotNull();
        assertThat(csrf).isNotNull();
        jdbc.update("DELETE FROM email_outbox WHERE email_type = 'WELCOME' AND user_id = (SELECT id FROM users WHERE email = ?)", email);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var futures = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timed out");
                return mvc.perform(post("/api/auth/welcome-email/resend").cookie(access, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())).andReturn().getResponse().getStatus();
            })).toList();
            start.countDown();
            var statuses = List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(202, 429);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void authenticatedUserCanRequestWelcomeAfterOldOutboxEntryWasRemoved() throws Exception {
        String email = "resend-missing-entry@example.test";
        MvcResult registration = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"Valid123\"}"))
                .andExpect(status().isCreated()).andReturn();
        Cookie access = registration.getResponse().getCookie("ACCESS_TOKEN");
        Cookie csrf = registration.getResponse().getCookie("XSRF-TOKEN");
        assertThat(access).isNotNull();
        assertThat(csrf).isNotNull();
        jdbc.update("DELETE FROM email_outbox WHERE email_type = 'WELCOME' AND user_id = (SELECT id FROM users WHERE email = ?)", email);

        mvc.perform(post("/api/auth/welcome-email/resend").cookie(access, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isAccepted())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());
    }
}
