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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
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
    void rejectsDeliveryCompletionWhenTheLeaseExpiresDuringSmtpSubmission() {
        Instant pollStart = Instant.parse("2026-08-24T15:00:00Z");
        Instant afterLeaseExpiry = pollStart.plus(Duration.ofMinutes(6));
        AtomicReference<Instant> currentTime = new AtomicReference<>(pollStart);
        Clock advancingClock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return currentTime.get();
            }
        };
        EmailOutboxDispatcher advancingDispatcher = new EmailOutboxDispatcher(
                transactions, transport, properties, advancingClock);
        EmailMessage stored = new EmailMessage("ana@example.com", "Stored subject", "<p>Stored body</p>", "WELCOME", "42");
        when(transactions.claimWelcomeBatch(pollStart)).thenReturn(List.of(
                new EmailOutboxTransactionService.ClaimedEmail(7L, "lease-1", 0, stored)));
        doAnswer(invocation -> {
            currentTime.set(afterLeaseExpiry);
            return null;
        }).when(transport).submit(stored);

        advancingDispatcher.dispatch();

        verify(transactions).markDelivered(7L, "lease-1", afterLeaseExpiry);
    }

    @Test
    void usesTheCurrentClockForRetryAndTerminalFailureCompletions() {
        Instant pollStart = Instant.parse("2026-08-24T15:00:00Z");
        AtomicReference<Instant> currentTime = new AtomicReference<>(pollStart);
        Clock advancingClock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return currentTime.get();
            }
        };
        EmailOutboxDispatcher advancingDispatcher = new EmailOutboxDispatcher(
                transactions, transport, properties, advancingClock);
        EmailMessage retryable = new EmailMessage("ana@example.com", "Retry", "<p>Retry</p>", "WELCOME", "42");
        EmailMessage permanent = new EmailMessage("bea@example.com", "Failed", "<p>Failed</p>", "WELCOME", "43");
        when(transactions.claimWelcomeBatch(pollStart)).thenReturn(List.of(
                new EmailOutboxTransactionService.ClaimedEmail(7L, "lease-1", 0, retryable),
                new EmailOutboxTransactionService.ClaimedEmail(8L, "lease-2", 0, permanent)));
        AtomicInteger submissions = new AtomicInteger();
        doAnswer(invocation -> {
            currentTime.set(pollStart.plus(Duration.ofMinutes(6 + submissions.getAndIncrement())));
            EmailMessage submitted = invocation.getArgument(0);
            throw new EmailTransportFailure(submitted == retryable
                    ? EmailTransportFailureClassification.SMTP_UNAVAILABLE
                    : EmailTransportFailureClassification.INVALID_STORED_PAYLOAD);
        }).when(transport).submit(org.mockito.ArgumentMatchers.any());

        advancingDispatcher.dispatch();

        verify(transactions).releaseForRetry(7L, "lease-1", EmailTransportFailureClassification.SMTP_UNAVAILABLE,
                pollStart.plus(Duration.ofMinutes(8)));
        verify(transactions).markFailed(8L, "lease-2", EmailTransportFailureClassification.INVALID_STORED_PAYLOAD,
                pollStart.plus(Duration.ofMinutes(7)));
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

    @Test
    void retryableFailureAtMaxAttemptsBecomesTerminalFailed() {
        EmailMessage stored = new EmailMessage("ana@example.com", "Stored subject", "<p>Stored body</p>", "WELCOME", "42");
        when(transactions.claimWelcomeBatch(clock.instant())).thenReturn(List.of(
                new EmailOutboxTransactionService.ClaimedEmail(7L, "lease-1", 2, stored)));
        doThrow(new EmailTransportFailure(EmailTransportFailureClassification.SMTP_UNAVAILABLE))
                .when(transport).submit(stored);

        dispatcher.dispatch();

        verify(transactions).markFailed(7L, "lease-1", EmailTransportFailureClassification.SMTP_UNAVAILABLE,
                clock.instant());
    }

    @Test
    void emptyPollSubmitsNothingAndLaterPollStillProcessesEligibleWork() {
        EmailMessage stored = new EmailMessage("ana@example.com", "Stored subject", "<p>Stored body</p>", "WELCOME", "42");
        when(transactions.claimWelcomeBatch(clock.instant())).thenReturn(List.of(), List.of(
                new EmailOutboxTransactionService.ClaimedEmail(7L, "lease-1", 0, stored)));

        dispatcher.dispatch();

        verifyNoInteractions(transport);
        dispatcher.dispatch();

        verify(transport).submit(stored);
        verify(transactions).markDelivered(7L, "lease-1", clock.instant());
    }

    @Test
    void acceptedSubmissionCanBeSubmittedAgainAfterItsCompletionIsRejected() {
        EmailMessage stored = new EmailMessage("ana@example.com", "Stored subject", "<p>Stored body</p>", "WELCOME", "42");
        when(transactions.claimWelcomeBatch(clock.instant())).thenReturn(
                List.of(new EmailOutboxTransactionService.ClaimedEmail(7L, "expired-lease", 0, stored)),
                List.of(new EmailOutboxTransactionService.ClaimedEmail(7L, "reclaimed-lease", 0, stored)));
        when(transactions.markDelivered(7L, "expired-lease", clock.instant())).thenReturn(0);
        when(transactions.markDelivered(7L, "reclaimed-lease", clock.instant())).thenReturn(1);

        dispatcher.dispatch();
        dispatcher.dispatch();

        verify(transport, times(2)).submit(stored);
        verify(transactions).markDelivered(7L, "reclaimed-lease", clock.instant());
    }

    private EmailOutboxProperties properties() {
        EmailOutboxProperties properties = new EmailOutboxProperties();
        properties.setMaxAttempts(3);
        properties.setBackoff(List.of(Duration.ofMinutes(2), Duration.ofMinutes(4)));
        return properties;
    }
}
