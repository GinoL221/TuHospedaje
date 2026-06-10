package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "app.mail.smtp.enabled", havingValue = "true")
public class SmtpEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailServiceImpl.class);

    private final JavaMailSender mailSender;

    public SmtpEmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
