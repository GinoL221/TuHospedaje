package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.WelcomeEmailProperties;
import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.EmailOutboxStatus;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.service.EmailOutboxService.WelcomeResendResult;
import com.tuhospedaje.service.WelcomeEmailRenderer;
import com.tuhospedaje.service.impl.EmailOutboxServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WelcomeResendServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-25T12:00:00Z");

    @Test
    void requeuesOnlyAnOldTerminalWelcomeRow() {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        EmailOutbox outbox = welcome(EmailOutboxStatus.DELIVERED, NOW.minus(Duration.ofMinutes(6)));
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7")).thenReturn(Optional.of(outbox));
        when(repository.requeueWelcomeIfTerminalAndCooled("7", NOW.minus(Duration.ofMinutes(5)))).thenReturn(1);

        WelcomeResendResult result = service(repository).resendWelcome(user(7L));

        assertThat(result).isEqualTo(WelcomeResendResult.SCHEDULED);
    }

    @Test
    void rejectsPendingAndRecentTerminalWelcomeRowsWithCooldown() {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        EmailOutbox pending = welcome(EmailOutboxStatus.PENDING, null);
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7")).thenReturn(Optional.of(pending));

        assertThat(service(repository).resendWelcome(user(7L))).isEqualTo(WelcomeResendResult.COOLDOWN);

        EmailOutbox recent = welcome(EmailOutboxStatus.DELIVERED, NOW.minus(Duration.ofMinutes(1)));
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7")).thenReturn(Optional.of(recent));
        assertThat(service(repository).resendWelcome(user(7L))).isEqualTo(WelcomeResendResult.COOLDOWN);
    }

    private EmailOutboxServiceImpl service(EmailOutboxRepository repository) {
        WelcomeEmailProperties properties = new WelcomeEmailProperties();
        properties.setResendCooldown(Duration.ofMinutes(5));
        return new EmailOutboxServiceImpl(repository, mock(WelcomeEmailRenderer.class), properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private EmailOutbox welcome(EmailOutboxStatus status, Instant completedAt) {
        EmailOutbox outbox = new EmailOutbox();
        outbox.setStatus(status);
        outbox.setCompletedAt(completedAt);
        return outbox;
    }
}
