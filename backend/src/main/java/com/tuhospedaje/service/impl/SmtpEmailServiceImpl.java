package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.service.EmailService;
import com.tuhospedaje.service.EmailTransport;
import com.tuhospedaje.service.EmailTransportFailure;
import com.tuhospedaje.service.EmailTransportFailureClassification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "app.mail.smtp.enabled", havingValue = "true")
public class SmtpEmailServiceImpl implements EmailService, EmailTransport {

    private final JavaMailSender mailSender;

    public SmtpEmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void submit(EmailMessage message) {
        validateStoredPayload(message);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.htmlBody(), true);
            mailSender.send(mimeMessage);
        } catch (MailAuthenticationException exception) {
            throw new EmailTransportFailure(EmailTransportFailureClassification.SMTP_AUTHENTICATION_REJECTED);
        } catch (MailException exception) {
            throw new EmailTransportFailure(EmailTransportFailureClassification.SMTP_UNAVAILABLE);
        } catch (MessagingException exception) {
            throw new EmailTransportFailure(EmailTransportFailureClassification.INVALID_STORED_PAYLOAD);
        }
    }

    @Override
    public void sendWelcomeEmail(RegisterRequest request) {
        String subject = "Welcome to TuHospedaje!";
        String body = """
                <html><body style="font-family:sans-serif;color:#222;">
                <h2 style="color:#c0392b;">Welcome to TuHospedaje, %s!</h2>
                <p>Thanks for registering. Your account is ready.</p>
                <p>Start exploring lodgings at <a href="http://localhost:5173">TuHospedaje</a>.</p>
                <hr><p style="font-size:12px;color:#888;">TuHospedaje &mdash; Your next stay, confirmed.</p>
                </body></html>
                """.formatted(request.getFirstName());

        send(request.getEmail(), subject, body);
    }

    @Override
    public void sendReservationConfirmation(ReservationResponse reservation) {
        String subject = "Booking confirmed — " + reservation.getLodgingName();
        String body = """
                <html><body style="font-family:sans-serif;color:#222;">
                <h2 style="color:#c0392b;">Your booking is confirmed!</h2>
                <table style="border-collapse:collapse;width:100%%">
                  <tr><td style="padding:6px 12px;font-weight:bold;">Lodging</td><td>%s — %s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Check-in</td><td>%s</td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Check-out</td><td>%s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Guest</td><td>%s</td></tr>
                  %s
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
                confirmationNotesRow(reservation),
                reservation.getGuestPhone() != null ? reservation.getGuestPhone() : "-",
                reservation.getTotalPrice(),
                reservation.getStatus(),
                reservation.getLodgingPhone() != null ? reservation.getLodgingPhone() : "-",
                reservation.getLodgingEmail() != null ? reservation.getLodgingEmail() : "-"
        );

        send(reservation.getGuestEmail(), subject, body);
    }

    @Override
    public void sendReservationCancellation(ReservationResponse reservation) {
        String subject = "Booking cancelled — " + reservation.getLodgingName();
        String body = """
                <html><body style="font-family:sans-serif;color:#222;">
                <h2>Your booking was cancelled</h2>
                <p>Your reservation at %s from %s to %s is now cancelled.</p>
                </body></html>
                """.formatted(reservation.getLodgingName(), reservation.getCheckIn(), reservation.getCheckOut());
        send(reservation.getGuestEmail(), subject, body);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            // MessagingException covers message construction (checked); MailException
            // (unchecked) is what JavaMailSender#send actually throws on real SMTP failures
            // (e.g. MailSendException) — without this branch, an SMTP outage propagated as
            // a RuntimeException straight out of an @Transactional caller.
        }
    }

    private void validateStoredPayload(EmailMessage message) {
        if (message == null || isBlankOrTooLong(message.to(), 256)
                || isBlankOrTooLong(message.subject(), 256) || isBlankOrTooLong(message.htmlBody(), 65535)) {
            throw new EmailTransportFailure(EmailTransportFailureClassification.INVALID_STORED_PAYLOAD);
        }
        try {
            InternetAddress address = new InternetAddress(message.to(), true);
            address.validate();
        } catch (AddressException exception) {
            throw new EmailTransportFailure(EmailTransportFailureClassification.INVALID_STORED_PAYLOAD);
        }
    }

    private boolean isBlankOrTooLong(String value, int maximumLength) {
        return value == null || value.isBlank() || value.length() > maximumLength;
    }

    private String confirmationNotesRow(ReservationResponse reservation) {
        String notes = reservation.getNotes();
        if (notes == null || notes.isBlank()) {
            return "";
        }
        return "<tr><td style=\"padding:6px 12px;font-weight:bold;\">Notes</td><td>%s</td></tr>"
                .formatted(escapeHtml(notes.trim()));
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
