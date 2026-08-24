package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.EmailOutboxProperties;
import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.service.EmailTransport;
import com.tuhospedaje.service.EmailTransportFailure;
import com.tuhospedaje.service.EmailTransportFailureClassification;
import com.tuhospedaje.service.impl.EmailOutboxDispatcher;
import com.tuhospedaje.service.impl.EmailOutboxTransactionService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailOutboxDispatcherTest {

    private final EmailOutboxTransactionService transactions = mock(EmailOutboxTransactionService.class);
    private final EmailTransport transport = mock(EmailTransport.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-24T15:00:00Z"), ZoneOffset.UTC);
    private final EmailOutboxProperties properties = properties();
    private final EmailOutboxDispatcher dispatcher = new EmailOutboxDispatcher(transactions, transport, properties, clock);

    @Test
    void submitsStoredWelcomePayloadAndMarksOnlyItsLeaseDelivered() {
        EmailMessage stored = new EmailMessage("ana@example.com", "Stored subject", "<p>Stored body</p>", "WELCOME", "42");
        when(transactions.claimWelcomeBatch(clock.instant())).thenReturn(List.of(
                new EmailOutboxTransactionService.ClaimedEmail(7L, "lease-1", 0, stored)));

        dispatcher.dispatch();

        verify(transport).submit(stored);
        verify(transactions).markDelivered(7L, "lease-1", clock.instant());
    }

    @Test
    void schedulesConfiguredBackoffForRetryableTransportFailure() {
        EmailMessage stored = new EmailMessage("ana@example.com", "Stored subject", "<p>Stored body</p>", "WELCOME", "42");
        when(transactions.claimWelcomeBatch(clock.instant())).thenReturn(List.of(
                new EmailOutboxTransactionService.ClaimedEmail(7L, "lease-1", 0, stored)));
        doThrow(new EmailTransportFailure(EmailTransportFailureClassification.SMTP_UNAVAILABLE))
                .when(transport).submit(stored);

        dispatcher.dispatch();

        verify(transactions).releaseForRetry(
                7L,
                "lease-1",
                EmailTransportFailureClassification.SMTP_UNAVAILABLE,
                clock.instant().plus(Duration.ofMinutes(2)));
    }

    @Test
    void marksPermanentTransportFailureAsTerminal() {
        EmailMessage stored = new EmailMessage("ana@example.com", "Stored subject", "<p>Stored body</p>", "WELCOME", "42");
        when(transactions.claimWelcomeBatch(clock.instant())).thenReturn(List.of(
                new EmailOutboxTransactionService.ClaimedEmail(7L, "lease-1", 0, stored)));
        doThrow(new EmailTransportFailure(EmailTransportFailureClassification.INVALID_STORED_PAYLOAD))
                .when(transport).submit(stored);

        dispatcher.dispatch();

        verify(transactions).markFailed(
                7L,
                "lease-1",
                EmailTransportFailureClassification.INVALID_STORED_PAYLOAD,
                clock.instant());
    }

    private EmailOutboxProperties properties() {
        EmailOutboxProperties properties = new EmailOutboxProperties();
        properties.setMaxAttempts(3);
        properties.setBackoff(List.of(Duration.ofMinutes(2), Duration.ofMinutes(4)));
        return properties;
    }
}
