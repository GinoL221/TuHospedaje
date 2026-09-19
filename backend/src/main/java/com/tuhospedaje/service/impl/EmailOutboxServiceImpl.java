package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.configuration.WelcomeEmailProperties;
import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.EmailOutboxStatus;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.service.EmailOutboxService;
import com.tuhospedaje.service.EmailOutboxService.WelcomeResendResult;
import com.tuhospedaje.service.EmailTemplateRenderer;
import com.tuhospedaje.service.WelcomeEmailRenderer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class EmailOutboxServiceImpl implements EmailOutboxService {

    private static final String WELCOME = "WELCOME";

    private final EmailOutboxRepository repository;
    private final WelcomeEmailRenderer welcomeEmailRenderer;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final WelcomeEmailProperties welcomeEmailProperties;
    private final Clock clock;

    @Autowired
    public EmailOutboxServiceImpl(EmailOutboxRepository repository, WelcomeEmailRenderer welcomeEmailRenderer,
                                  EmailTemplateRenderer emailTemplateRenderer,
                                  WelcomeEmailProperties welcomeEmailProperties, Clock clock) {
        this.repository = repository;
        this.welcomeEmailRenderer = welcomeEmailRenderer;
        this.emailTemplateRenderer = emailTemplateRenderer;
        this.welcomeEmailProperties = welcomeEmailProperties;
        this.clock = clock;
    }

    public EmailOutboxServiceImpl(EmailOutboxRepository repository, WelcomeEmailRenderer welcomeEmailRenderer,
                                  WelcomeEmailProperties welcomeEmailProperties, Clock clock) {
        this(repository, welcomeEmailRenderer, new EmailTemplateRenderer(), welcomeEmailProperties, clock);
    }

    public EmailOutboxServiceImpl(EmailOutboxRepository repository, WelcomeEmailRenderer welcomeEmailRenderer) {
        this(repository, welcomeEmailRenderer, new EmailTemplateRenderer(), new WelcomeEmailProperties(), Clock.systemUTC());
    }

    @Override
    @Transactional
    public void enqueueWelcome(User user, RegisterRequest request) {
        EmailMessage message = welcomeEmailRenderer.render(user.getId(), request.getEmail(), request.getFirstName());
        enqueue(user, message.emailType(), message.aggregateId(), message.to(), message.subject(), message.htmlBody(), true);
    }

    @Override
    @Transactional
    public WelcomeResendResult resendWelcome(User user) {
        String aggregateId = user.getId().toString();
        if (repository.findByEmailTypeAndAggregateId(WELCOME, aggregateId).isEmpty()) {
            return WelcomeResendResult.COOLDOWN;
        }
        int requeued = repository.requeueWelcomeIfTerminalAndCooled(aggregateId,
                clock.instant().minus(welcomeEmailProperties.getResendCooldown()));
        return requeued == 1 ? WelcomeResendResult.SCHEDULED : WelcomeResendResult.COOLDOWN;
    }

    @Override
    @Transactional
    public void enqueueReservationConfirmation(User user, ReservationResponse reservation) {
        EmailMessage message = emailTemplateRenderer.renderReservationConfirmation(user, reservation);
        enqueue(user, message.emailType(), message.aggregateId(), message.to(), message.subject(), message.htmlBody(), false);
    }

    @Override
    @Transactional
    public void enqueueReservationCancellation(User user, ReservationResponse reservation) {
        EmailMessage message = emailTemplateRenderer.renderReservationCancellation(user, reservation);
        enqueue(user, message.emailType(), message.aggregateId(), message.to(), message.subject(), message.htmlBody(), false);
    }

    private void enqueue(User user, String emailType, String aggregateId, String recipient,
                         String subject, String htmlBody, boolean flush) {
        if (repository.findByEmailTypeAndAggregateId(emailType, aggregateId).isPresent()) {
            return;
        }

        EmailOutbox outbox = new EmailOutbox();
        outbox.setUser(user);
        outbox.setEmailType(emailType);
        outbox.setAggregateId(aggregateId);
        outbox.setRecipient(recipient);
        outbox.setSubject(subject);
        outbox.setHtmlBody(htmlBody);
        outbox.setStatus(EmailOutboxStatus.PENDING);
        outbox.setFailedAttempts(0);
        if (flush) {
            repository.saveAndFlush(outbox);
        } else {
            repository.save(outbox);
        }
    }

}
