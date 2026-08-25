package com.tuhospedaje.service.impl;

import com.tuhospedaje.enums.EmailOutboxType;
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
    public void poll() {
        EmailOutboxDispatcher availableDispatcher = dispatcher.getIfAvailable();
        if (availableDispatcher != null) {
            for (EmailOutboxType type : EmailOutboxType.values()) {
                availableDispatcher.dispatch(type);
            }
        }
    }

    public void pollWelcome() {
        poll();
    }

    @Scheduled(fixedDelayString = "${tuhospedaje.email-outbox.cleanup-interval}")
    public void cleanup() {
        for (EmailOutboxType type : EmailOutboxType.values()) {
            try {
                int deleted = transactions.cleanup(type);
                log.info("event=email_outbox.cleanup_completed email_type={} deleted_count={}", type, deleted);
            } catch (RuntimeException ignored) {
                log.warn("event=email_outbox.cleanup_failed email_type={} classification=CLEANUP_FAILED", type);
            }
        }
    }

    public void cleanupWelcome() {
        cleanup();
    }
}
