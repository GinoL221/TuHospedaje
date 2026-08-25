package com.tuhospedaje.service.impl;

import com.tuhospedaje.configuration.EmailOutboxProperties;
import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.enums.EmailOutboxStatus;
import com.tuhospedaje.enums.EmailOutboxType;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.service.EmailTransportFailureClassification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class EmailOutboxTransactionService {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxTransactionService.class);

    private final EmailOutboxRepository repository;
    private final EmailOutboxProperties properties;
    private final Clock clock;

    public EmailOutboxTransactionService(EmailOutboxRepository repository, EmailOutboxProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public List<ClaimedEmail> claimBatch(EmailOutboxType type, Instant now) {
        String token = UUID.randomUUID().toString();
        Instant leaseUntil = now.plus(properties.getLeaseDuration());
        repository.claimEligible(type.name(), now, properties.getBatchSize(), token, leaseUntil);
        List<ClaimedEmail> claimed = repository.findByStatusAndLeaseToken(EmailOutboxStatus.PROCESSING, token).stream()
                .map(outbox -> snapshot(outbox, token))
                .toList();
        if (claimed.isEmpty()) {
            log.debug("event=email_outbox.poll_empty email_type={}", type);
        } else {
            log.info("event=email_outbox.claimed email_type={} claimed_count={}", type, claimed.size());
        }
        return claimed;
    }

    @Transactional
    public List<ClaimedEmail> claimWelcomeBatch(Instant now) {
        return claimBatch(EmailOutboxType.WELCOME, now);
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int cleanup(EmailOutboxType type) {
        return repository.purgeCompletedBefore(type.name(), clock.instant().minus(properties.getRetention()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int cleanupWelcome() {
        return cleanup(EmailOutboxType.WELCOME);
    }

    private ClaimedEmail snapshot(EmailOutbox outbox, String token) {
        return new ClaimedEmail(outbox.getId(), token, outbox.getFailedAttempts(), new EmailMessage(
                outbox.getRecipient(), outbox.getSubject(), outbox.getHtmlBody(),
                outbox.getEmailType(), outbox.getAggregateId()));
    }

    public record ClaimedEmail(long id, String token, int failedAttempts, EmailMessage message) {
    }
}
