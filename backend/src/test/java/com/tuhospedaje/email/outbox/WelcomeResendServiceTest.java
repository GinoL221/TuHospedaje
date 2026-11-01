package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.WelcomeEmailProperties;
import com.tuhospedaje.dto.email.EmailMessage;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;
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
    void distinguishesPendingFromRecentTerminalWelcomeRows() {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        EmailOutbox pending = welcome(EmailOutboxStatus.PENDING, null);
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7")).thenReturn(Optional.of(pending));

        assertThat(service(repository).resendWelcome(user(7L))).isEqualTo(WelcomeResendResult.ALREADY_PENDING);

        EmailOutbox recent = welcome(EmailOutboxStatus.DELIVERED, NOW.minus(Duration.ofMinutes(1)));
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7")).thenReturn(Optional.of(recent));
        when(repository.lockWelcomeStatus("7")).thenReturn(Optional.of("DELIVERED"));
        assertThat(service(repository).resendWelcome(user(7L))).isEqualTo(WelcomeResendResult.COOLDOWN);
    }

    @Test
    void schedulesWelcomeWhenPreviousOutboxEntryIsMissing() {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7")).thenReturn(Optional.empty());

        User user = user(7L);
        user.setFirstName("Ana");
        user.setEmail("ana@example.test");
        WelcomeEmailRenderer renderer = mock(WelcomeEmailRenderer.class);
        when(renderer.render(7L, "ana@example.test", "Ana"))
                .thenReturn(new EmailMessage("ana@example.test", "Welcome", "Hello Ana", "WELCOME", "7"));
        WelcomeEmailProperties properties = new WelcomeEmailProperties();
        EmailOutboxServiceImpl service = new EmailOutboxServiceImpl(repository, renderer, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.resendWelcome(user)).isEqualTo(WelcomeResendResult.SCHEDULED);
        ArgumentCaptor<EmailOutbox> entry = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(repository).saveAndFlush(entry.capture());
        assertThat(entry.getValue().getRecipient()).isEqualTo("ana@example.test");
        assertThat(entry.getValue().getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
    }

    @Test
    void blocksBeforeFiveMinutesEvenWhenTheOutboxEntryIsAbsent() {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        User user = user(7L);
        user.setWelcomeResendRequestedAt(NOW.minusSeconds(299));

        assertThat(service(repository).resendWelcome(user)).isEqualTo(WelcomeResendResult.COOLDOWN);
        assertThat(user.getWelcomeResendRequestedAt()).isEqualTo(NOW.minusSeconds(299));
        verifyNoInteractions(repository);
    }

    @Test
    void allowsAtFiveMinuteBoundaryWhenPreviousEntryIsMissing() {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7")).thenReturn(Optional.empty());
        User user = user(7L);
        user.setEmail("ana@example.test");
        user.setFirstName("Ana");
        user.setWelcomeResendRequestedAt(NOW.minusSeconds(300));
        WelcomeEmailRenderer renderer = mock(WelcomeEmailRenderer.class);
        when(renderer.render(7L, "ana@example.test", "Ana"))
                .thenReturn(new EmailMessage("ana@example.test", "Welcome", "Hello", "WELCOME", "7"));
        EmailOutboxServiceImpl service = new EmailOutboxServiceImpl(repository, renderer,
                new WelcomeEmailProperties(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.resendWelcome(user)).isEqualTo(WelcomeResendResult.SCHEDULED);
        assertThat(user.getWelcomeResendRequestedAt()).isEqualTo(NOW);
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any(EmailOutbox.class));
    }

    @Test
    void schedulesWhenCleanupRemovesTerminalEntryBeforeRequeue() {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        EmailOutbox old = welcome(EmailOutboxStatus.DELIVERED, NOW.minus(Duration.ofDays(31)));
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7")).thenReturn(Optional.of(old));
        when(repository.requeueWelcomeIfTerminalAndCooled("7", NOW.minus(Duration.ofMinutes(5))))
                .thenReturn(0);
        when(repository.lockWelcomeStatus("7")).thenReturn(Optional.empty());
        User user = user(7L);
        user.setFirstName("Ana");
        user.setEmail("ana@example.test");
        WelcomeEmailRenderer renderer = mock(WelcomeEmailRenderer.class);
        when(renderer.render(7L, "ana@example.test", "Ana"))
                .thenReturn(new EmailMessage("ana@example.test", "Welcome", "Hello", "WELCOME", "7"));
        EmailOutboxServiceImpl service = new EmailOutboxServiceImpl(repository, renderer,
                new WelcomeEmailProperties(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.resendWelcome(user)).isEqualTo(WelcomeResendResult.SCHEDULED);
        verify(repository).lockWelcomeStatus("7");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any(EmailOutbox.class));
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
