package com.tuhospedaje.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(prefix = "tuhospedaje.email-outbox", name = "enabled", havingValue = "true")
public class EmailOutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxScheduler.class);

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
            int deleted = transactions.cleanupWelcome();
            log.info("event=email_outbox.cleanup_completed email_type=WELCOME deleted_count={}", deleted);
        } catch (RuntimeException ignored) {
            log.warn("event=email_outbox.cleanup_failed email_type=WELCOME classification=CLEANUP_FAILED");
        }
    }
}
