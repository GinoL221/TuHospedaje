package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WelcomeOutboxEnqueueRollbackIntegrationTest {

    private static final String EMAIL = "wu1-outbox-rollback@test.com";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private EmailOutboxRepository emailOutboxRepository;

    @BeforeEach
    @AfterEach
    void cleanFixtures() {
        reset(emailOutboxRepository);
        jdbcTemplate.update("DELETE FROM email_outbox WHERE user_id IN (SELECT id FROM users WHERE email = ?)", EMAIL);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", EMAIL);
    }

    @Test
    void realWelcomeOutboxPersistenceFailureRollsBackRegistration() {
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(emailOutboxRepository).saveAndFlush(any(EmailOutbox.class));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ana", "Gómez", EMAIL, "secret123")))
                .isInstanceOf(RuntimeException.class);

        assertThat(userRepository.findByEmail(EMAIL)).isEmpty();
        assertThat(emailOutboxRepository.findAll()).isEmpty();
    }
}
