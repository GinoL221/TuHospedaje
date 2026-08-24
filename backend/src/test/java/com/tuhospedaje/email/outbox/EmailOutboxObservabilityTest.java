package com.tuhospedaje.email.outbox;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tuhospedaje.configuration.EmailOutboxProperties;
import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.enums.EmailOutboxStatus;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.service.EmailTransport;
import com.tuhospedaje.service.EmailTransportFailure;
import com.tuhospedaje.service.EmailTransportFailureClassification;
import com.tuhospedaje.service.impl.EmailOutboxDispatcher;
import com.tuhospedaje.service.impl.EmailOutboxScheduler;
import com.tuhospedaje.service.impl.EmailOutboxTransactionService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailOutboxObservabilityTest {

    private static final Instant NOW = Instant.parse("2026-08-24T15:00:00Z");
    private static final String SENSITIVE_RECIPIENT = "recipient@example.com";
    private static final String SENSITIVE_BODY = "smtp-password=secret-token";

    @Test
    void emitsOnlySafeClaimEventFields() {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        EmailOutboxProperties properties = mock(EmailOutboxProperties.class);
        EmailOutbox outbox = mock(EmailOutbox.class);
        when(properties.getLeaseDuration()).thenReturn(Duration.ofMinutes(5));
        when(properties.getBatchSize()).thenReturn(1);
        when(repository.findByStatusAndLeaseToken(org.mockito.ArgumentMatchers.eq(EmailOutboxStatus.PROCESSING),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(outbox));
        when(outbox.getId()).thenReturn(7L);
        when(outbox.getFailedAttempts()).thenReturn(0);
        when(outbox.getRecipient()).thenReturn(SENSITIVE_RECIPIENT);
        when(outbox.getSubject()).thenReturn("Sensitive subject");
        when(outbox.getHtmlBody()).thenReturn(SENSITIVE_BODY);
        when(outbox.getEmailType()).thenReturn("WELCOME");
        when(outbox.getAggregateId()).thenReturn("42");

        ListAppender<ILoggingEvent> events = attach(EmailOutboxTransactionService.class);
        try {
            new EmailOutboxTransactionService(repository, properties, Clock.fixed(NOW, ZoneOffset.UTC)).claimWelcomeBatch(NOW);

            assertThat(messages(events)).containsExactly("event=email_outbox.claimed email_type=WELCOME claimed_count=1");
        } finally {
            detach(EmailOutboxTransactionService.class, events);
        }
    }

    @Test
    void emitsSmtpAcceptanceRetryTerminalAndStaleOwnerEventsWithoutPayloadOrMailboxDeliveryClaims() {
        EmailOutboxTransactionService transactions = mock(EmailOutboxTransactionService.class);
        EmailTransport transport = mock(EmailTransport.class);
        EmailOutboxProperties properties = properties();
        EmailMessage message = new EmailMessage(SENSITIVE_RECIPIENT, "Sensitive subject", SENSITIVE_BODY, "WELCOME", "42");
        when(transactions.claimWelcomeBatch(NOW)).thenReturn(List.of(
                new EmailOutboxTransactionService.ClaimedEmail(7L, "lease-1", 0, message),
                new EmailOutboxTransactionService.ClaimedEmail(8L, "lease-2", 0, message),
                new EmailOutboxTransactionService.ClaimedEmail(9L, "lease-3", 2, message),
                new EmailOutboxTransactionService.ClaimedEmail(10L, "lease-4", 0, message)));
        when(transactions.markDelivered(7L, "lease-1", NOW)).thenReturn(1);
        when(transactions.markDelivered(10L, "lease-4", NOW)).thenReturn(0);
        org.mockito.Mockito.doNothing()
                .doThrow(new EmailTransportFailure(EmailTransportFailureClassification.SMTP_UNAVAILABLE))
                .doThrow(new EmailTransportFailure(EmailTransportFailureClassification.INVALID_STORED_PAYLOAD))
                .doNothing()
                .when(transport).submit(message);
        when(transactions.releaseForRetry(8L, "lease-2", EmailTransportFailureClassification.SMTP_UNAVAILABLE,
                NOW.plus(Duration.ofMinutes(2)))).thenReturn(1);
        when(transactions.markFailed(9L, "lease-3", EmailTransportFailureClassification.INVALID_STORED_PAYLOAD, NOW))
                .thenReturn(1);

        ListAppender<ILoggingEvent> events = attach(EmailOutboxDispatcher.class);
        try {
            new EmailOutboxDispatcher(transactions, transport, properties, Clock.fixed(NOW, ZoneOffset.UTC)).dispatch();

            assertThat(messages(events)).containsExactly(
                    "event=email_outbox.smtp_accepted email_type=WELCOME outbox_id=7 aggregate_id=42 updated=1",
                    "event=email_outbox.retry_scheduled email_type=WELCOME outbox_id=8 aggregate_id=42 attempt=1 max_attempts=3 classification=SMTP_UNAVAILABLE updated=1",
                    "event=email_outbox.failed email_type=WELCOME outbox_id=9 aggregate_id=42 attempt=3 max_attempts=3 classification=INVALID_STORED_PAYLOAD updated=1",
                    "event=email_outbox.stale_owner_rejected email_type=WELCOME outbox_id=10 aggregate_id=42 state=DELIVERED");
            assertThat(messages(events)).noneMatch(messageText -> messageText.contains(SENSITIVE_RECIPIENT)
                    || messageText.contains(SENSITIVE_BODY) || messageText.contains("Sensitive subject"));
            assertThat(messages(events)).noneMatch(messageText -> messageText.contains("provider delivery")
                    || messageText.contains("mailbox arrival") || messageText.contains("recipient receipt")
                    || messageText.contains("message read"));
        } finally {
            detach(EmailOutboxDispatcher.class, events);
        }
    }

    @Test
    void emitsBoundedCleanupEventsWithoutRawFailureText() {
        EmailOutboxTransactionService transactions = mock(EmailOutboxTransactionService.class);
        ObjectProvider<EmailOutboxDispatcher> dispatcher = mock(ObjectProvider.class);
        when(transactions.cleanupWelcome()).thenReturn(3).thenThrow(new IllegalStateException(SENSITIVE_BODY));
        EmailOutboxScheduler scheduler = new EmailOutboxScheduler(dispatcher, transactions);

        ListAppender<ILoggingEvent> events = attach(EmailOutboxScheduler.class);
        try {
            scheduler.cleanupWelcome();
            scheduler.cleanupWelcome();

            assertThat(messages(events)).containsExactly(
                    "event=email_outbox.cleanup_completed email_type=WELCOME deleted_count=3",
                    "event=email_outbox.cleanup_failed email_type=WELCOME classification=CLEANUP_FAILED");
            assertThat(messages(events)).noneMatch(message -> message.contains(SENSITIVE_BODY));
        } finally {
            detach(EmailOutboxScheduler.class, events);
        }
    }

    private EmailOutboxProperties properties() {
        EmailOutboxProperties properties = new EmailOutboxProperties();
        properties.setMaxAttempts(3);
        properties.setBackoff(List.of(Duration.ofMinutes(2), Duration.ofMinutes(4)));
        return properties;
    }

    private ListAppender<ILoggingEvent> attach(Class<?> source) {
        Logger logger = (Logger) LoggerFactory.getLogger(source);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        return events;
    }

    private void detach(Class<?> source, ListAppender<ILoggingEvent> events) {
        ((Logger) LoggerFactory.getLogger(source)).detachAppender(events);
        events.stop();
    }

    private List<String> messages(ListAppender<ILoggingEvent> events) {
        return events.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
