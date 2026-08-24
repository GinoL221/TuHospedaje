package com.tuhospedaje.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "tuhospedaje.email-outbox", name = "enabled", havingValue = "true")
public class EmailOutboxScheduler {

    private final ObjectProvider<EmailOutboxDispatcher> dispatcher;
    private final EmailOutboxTransactionService transactions;

    public EmailOutboxScheduler(ObjectProvider<EmailOutboxDispatcher> dispatcher, EmailOutboxTransactionService transactions) {
        this.dispatcher = dispatcher;
        this.transactions = transactions;
    }

    @Scheduled(fixedDelayString = "${tuhospedaje.email-outbox.poll-interval}")
    public void pollWelcome() {
        EmailOutboxDispatcher availableDispatcher = dispatcher.getIfAvailable();
        if (availableDispatcher != null) {
            availableDispatcher.dispatch();
        }
    }

    @Scheduled(fixedDelayString = "${tuhospedaje.email-outbox.cleanup-interval}")
    public void cleanupWelcome() {
        try {
            transactions.cleanupWelcome();
        } catch (RuntimeException ignored) {
            // Cleanup is isolated from dispatch and may retry on its next scheduled run.
        }
    }
}
