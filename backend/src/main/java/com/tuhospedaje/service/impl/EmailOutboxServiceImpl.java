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
import com.tuhospedaje.service.WelcomeEmailRenderer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class EmailOutboxServiceImpl implements EmailOutboxService {

    private static final String WELCOME = "WELCOME";
    private static final String RESERVATION_CONFIRMATION = "RESERVATION_CONFIRMATION";
    private static final String RESERVATION_CANCELLATION = "RESERVATION_CANCELLATION";

    private final EmailOutboxRepository repository;
    private final WelcomeEmailRenderer welcomeEmailRenderer;
    private final WelcomeEmailProperties welcomeEmailProperties;
    private final Clock clock;

    @Autowired
    public EmailOutboxServiceImpl(EmailOutboxRepository repository, WelcomeEmailRenderer welcomeEmailRenderer,
                                  WelcomeEmailProperties welcomeEmailProperties, Clock clock) {
        this.repository = repository;
        this.welcomeEmailRenderer = welcomeEmailRenderer;
        this.welcomeEmailProperties = welcomeEmailProperties;
        this.clock = clock;
    }

    public EmailOutboxServiceImpl(EmailOutboxRepository repository, WelcomeEmailRenderer welcomeEmailRenderer) {
        this(repository, welcomeEmailRenderer, new WelcomeEmailProperties(), Clock.systemUTC());
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
        String subject = "Booking confirmed — " + reservation.getLodgingName();
        String body = """
                <html><body style="font-family:sans-serif;color:#222;">
                <h2 style="color:#c0392b;">Your booking is confirmed!</h2>
                <table style="border-collapse:collapse;width:100%%">
                  <tr><td style="padding:6px 12px;font-weight:bold;">Lodging</td><td>%s — %s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Check-in</td><td>%s</td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Check-out</td><td>%s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Guest</td><td>%s</td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Phone</td><td>%s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Total</td><td><strong>$%s</strong></td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Status</td><td>%s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Contact phone</td><td>%s</td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Contact email</td><td>%s</td></tr>
                </table>
                <p style="margin-top:20px;">See you there!</p>
                <hr><p style="font-size:12px;color:#888;">TuHospedaje &mdash; Your next stay, confirmed.</p>
                </body></html>
                """.formatted(
                reservation.getLodgingName(),
                reservation.getCity(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getGuestName(),
                reservation.getGuestPhone() != null ? reservation.getGuestPhone() : "-",
                reservation.getTotalPrice(),
                reservation.getStatus(),
                reservation.getLodgingPhone() != null ? reservation.getLodgingPhone() : "-",
                reservation.getLodgingEmail() != null ? reservation.getLodgingEmail() : "-"
        );

        enqueue(user, RESERVATION_CONFIRMATION, reservation.getId().toString(),
                reservation.getGuestEmail(), subject, body, false);
    }

    @Override
    @Transactional
    public void enqueueReservationCancellation(User user, ReservationResponse reservation) {
        String subject = "Booking cancelled — " + reservation.getLodgingName();
        String body = """
                <html><body style="font-family:sans-serif;color:#222;">
                <h2>Your booking was cancelled</h2>
                <p>Your reservation at %s from %s to %s is now cancelled.</p>
                </body></html>
                """.formatted(reservation.getLodgingName(), reservation.getCheckIn(), reservation.getCheckOut());

        enqueue(user, RESERVATION_CANCELLATION, reservation.getId().toString(),
                reservation.getGuestEmail(), subject, body, false);
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
