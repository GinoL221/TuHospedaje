package com.tuhospedaje.service.impl;

import com.tuhospedaje.configuration.EmailOutboxProperties;
import com.tuhospedaje.service.EmailTransport;
import com.tuhospedaje.service.EmailTransportFailure;
import com.tuhospedaje.service.EmailTransportFailureClassification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@ConditionalOnBean(EmailTransport.class)
public class EmailOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxDispatcher.class);

    private final EmailOutboxTransactionService transactions;
    private final EmailTransport transport;
    private final EmailOutboxProperties properties;
    private final Clock clock;

    public EmailOutboxDispatcher(EmailOutboxTransactionService transactions, EmailTransport transport,
                                 EmailOutboxProperties properties, Clock clock) {
        this.transactions = transactions;
        this.transport = transport;
        this.properties = properties;
        this.clock = clock;
    }

    public void dispatch() {
        Instant now = clock.instant();
        for (EmailOutboxTransactionService.ClaimedEmail claimed : transactions.claimWelcomeBatch(now)) {
            submit(claimed, now);
        }
    }

    private void submit(EmailOutboxTransactionService.ClaimedEmail claimed, Instant outcomeTime) {
        try {
            transport.submit(claimed.message());
            int updated = transactions.markDelivered(claimed.id(), claimed.token(), outcomeTime);
            if (updated == 0) {
                logStaleOwner(claimed, "DELIVERED");
                return;
            }
            log.info("event=email_outbox.smtp_accepted email_type=WELCOME outbox_id={} aggregate_id={} updated={}",
                    claimed.id(), claimed.message().aggregateId(), updated);
        } catch (EmailTransportFailure failure) {
            completeFailure(claimed, failure.classification(), outcomeTime);
        }
    }

    private void completeFailure(EmailOutboxTransactionService.ClaimedEmail claimed,
                                 EmailTransportFailureClassification classification, Instant outcomeTime) {
        int attempt = claimed.failedAttempts() + 1;
        if (!classification.isRetryable() || attempt >= properties.getMaxAttempts()) {
            int updated = transactions.markFailed(claimed.id(), claimed.token(), classification, outcomeTime);
            if (updated == 0) {
                logStaleOwner(claimed, "FAILED");
                return;
            }
            log.info("event=email_outbox.failed email_type=WELCOME outbox_id={} aggregate_id={} attempt={} max_attempts={} classification={} updated={}",
                    claimed.id(), claimed.message().aggregateId(), attempt, properties.getMaxAttempts(), classification, updated);
            return;
        }
        int updated = transactions.releaseForRetry(claimed.id(), claimed.token(), classification,
                outcomeTime.plus(properties.getBackoff().get(attempt - 1)));
        if (updated == 0) {
            logStaleOwner(claimed, "PENDING");
            return;
        }
        log.info("event=email_outbox.retry_scheduled email_type=WELCOME outbox_id={} aggregate_id={} attempt={} max_attempts={} classification={} updated={}",
                claimed.id(), claimed.message().aggregateId(), attempt, properties.getMaxAttempts(), classification, updated);
    }

    private void logStaleOwner(EmailOutboxTransactionService.ClaimedEmail claimed, String state) {
        log.warn("event=email_outbox.stale_owner_rejected email_type=WELCOME outbox_id={} aggregate_id={} state={}",
                claimed.id(), claimed.message().aggregateId(), state);
    }
}
