package com.tuhospedaje.service.impl;

import com.tuhospedaje.configuration.EmailOutboxProperties;
import com.tuhospedaje.service.EmailTransport;
import com.tuhospedaje.service.EmailTransportFailure;
import com.tuhospedaje.service.EmailTransportFailureClassification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@ConditionalOnBean(EmailTransport.class)
public class EmailOutboxDispatcher {

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
            transactions.markDelivered(claimed.id(), claimed.token(), outcomeTime);
        } catch (EmailTransportFailure failure) {
            completeFailure(claimed, failure.classification(), outcomeTime);
        }
    }

    private void completeFailure(EmailOutboxTransactionService.ClaimedEmail claimed,
                                 EmailTransportFailureClassification classification, Instant outcomeTime) {
        int attempt = claimed.failedAttempts() + 1;
        if (!classification.isRetryable() || attempt >= properties.getMaxAttempts()) {
            transactions.markFailed(claimed.id(), claimed.token(), classification, outcomeTime);
            return;
        }
        transactions.releaseForRetry(claimed.id(), claimed.token(), classification,
                outcomeTime.plus(properties.getBackoff().get(attempt - 1)));
    }
}
