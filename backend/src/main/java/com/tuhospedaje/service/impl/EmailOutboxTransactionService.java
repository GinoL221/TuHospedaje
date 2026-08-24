package com.tuhospedaje.service.impl;

import com.tuhospedaje.configuration.EmailOutboxProperties;
import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.enums.EmailOutboxStatus;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.service.EmailTransportFailureClassification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EmailOutboxTransactionService {

    private static final String WELCOME = "WELCOME";

    private final EmailOutboxRepository repository;
    private final EmailOutboxProperties properties;

    public EmailOutboxTransactionService(EmailOutboxRepository repository, EmailOutboxProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public List<ClaimedEmail> claimWelcomeBatch(Instant now) {
        String token = UUID.randomUUID().toString();
        Instant leaseUntil = now.plus(properties.getLeaseDuration());
        repository.claimEligible(WELCOME, now, properties.getBatchSize(), token, leaseUntil);
        return repository.findByStatusAndLeaseToken(EmailOutboxStatus.PROCESSING, token).stream()
                .map(outbox -> snapshot(outbox, token))
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markDelivered(long id, String token, Instant outcomeTime) {
        return repository.markDelivered(id, token, outcomeTime);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int releaseForRetry(long id, String token, EmailTransportFailureClassification classification,
                               Instant nextAttemptAt) {
        return repository.releaseForRetry(id, token, classification.name(), nextAttemptAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markFailed(long id, String token, EmailTransportFailureClassification classification,
                          Instant outcomeTime) {
        return repository.markFailed(id, token, outcomeTime, classification.name());
    }

    private ClaimedEmail snapshot(EmailOutbox outbox, String token) {
        return new ClaimedEmail(outbox.getId(), token, outbox.getFailedAttempts(), new EmailMessage(
                outbox.getRecipient(), outbox.getSubject(), outbox.getHtmlBody(),
                outbox.getEmailType(), outbox.getAggregateId()));
    }

    public record ClaimedEmail(long id, String token, int failedAttempts, EmailMessage message) {
    }
}
