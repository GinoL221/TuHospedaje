package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, WelcomeResendBoundaryIntegrationTest.FixedClockConfiguration.class})
class WelcomeResendBoundaryIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-08-25T12:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock resendTestClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;
    @Autowired private com.tuhospedaje.repository.UserRepository users;
    @Autowired private org.springframework.transaction.support.TransactionTemplate transactions;

    @Test
    void cooldownBlocksAt299SecondsAndAllowsAt300Seconds() throws Exception {
        String email = "resend-boundary@example.test";
        MvcResult registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"Valid123\"}"))
                .andExpect(status().isCreated()).andReturn();
        Cookie access = registration.getResponse().getCookie("ACCESS_TOKEN");
        Cookie csrf = registration.getResponse().getCookie("XSRF-TOKEN");
        assertThat(access).isNotNull();
        assertThat(csrf).isNotNull();
        jdbc.update("DELETE FROM email_outbox WHERE email_type = 'WELCOME' AND user_id = (SELECT id FROM users WHERE email = ?)", email);

        Long userId = users.findByEmail(email).orElseThrow().getId();

        // Boundary test 1: Exactly 299 seconds elapsed -> still in cooldown -> 429
        transactions.executeWithoutResult(status -> users.updateWelcomeResendRequestedAt(userId, NOW.minusSeconds(299)));
        assertThat(users.findByEmail(email).orElseThrow().getWelcomeResendRequestedAt()).isEqualTo(NOW.minusSeconds(299));

        mvc.perform(post("/api/auth/welcome-email/resend").cookie(access, csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isTooManyRequests())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Retry-After")).isEqualTo("300"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM email_outbox WHERE email_type = 'WELCOME' AND user_id = ?", Integer.class, userId)).isZero();

        // Boundary test 2: Exactly 300 seconds elapsed -> cooldown expired -> 202
        transactions.executeWithoutResult(status -> users.updateWelcomeResendRequestedAt(userId, NOW.minusSeconds(300)));
        assertThat(users.findByEmail(email).orElseThrow().getWelcomeResendRequestedAt()).isEqualTo(NOW.minusSeconds(300));

        mvc.perform(post("/api/auth/welcome-email/resend").cookie(access, csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isAccepted())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM email_outbox WHERE email_type = 'WELCOME' AND status = 'PENDING' AND user_id = ?", Integer.class, userId)).isEqualTo(1);
        assertThat(users.findByEmail(email).orElseThrow().getWelcomeResendRequestedAt()).isEqualTo(NOW);
    }
}
