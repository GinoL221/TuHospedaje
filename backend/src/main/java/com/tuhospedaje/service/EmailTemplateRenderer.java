package com.tuhospedaje.service;

import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.User;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateRenderer {

    private static final String RESERVATION_CONFIRMATION = "RESERVATION_CONFIRMATION";
    private static final String RESERVATION_CANCELLATION = "RESERVATION_CANCELLATION";

    public EmailMessage renderReservationConfirmation(User user, ReservationResponse reservation) {
        String subject = "Booking confirmed — " + reservation.getLodgingName();
        String body = """
                <html><body style="font-family:sans-serif;color:#222;">
                <h2 style="color:#c0392b;">Your booking is confirmed!</h2>
                <table style="border-collapse:collapse;width:100%%">
                  <tr><td style="padding:6px 12px;font-weight:bold;">Lodging</td><td>%s — %s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Check-in</td><td>%s</td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Check-out</td><td>%s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Guest</td><td>%s</td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Reservation number</td><td>%s</td></tr>
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
                reservation.getLodgingName(), reservation.getCity(), reservation.getCheckIn(), reservation.getCheckOut(),
                reservation.getGuestName(), reservation.getId(), confirmationNotesRow(reservation),
                valueOrDash(reservation.getGuestPhone()), reservation.getTotalPrice(), reservation.getStatus(),
                valueOrDash(reservation.getLodgingPhone()), valueOrDash(reservation.getLodgingEmail()));
        return message(user, reservation, subject, body, RESERVATION_CONFIRMATION);
    }

    public EmailMessage renderReservationCancellation(User user, ReservationResponse reservation) {
        String subject = "Booking cancelled — " + reservation.getLodgingName();
        String body = """
                <html><body style="font-family:sans-serif;color:#222;">
                <h2>Your booking was cancelled</h2>
                <p>Your reservation at %s from %s to %s is now cancelled.</p>
                <p>Contact the lodging at %s or %s for further assistance.</p>
                </body></html>
                """.formatted(reservation.getLodgingName(), reservation.getCheckIn(), reservation.getCheckOut(),
                valueOrDash(reservation.getLodgingPhone()), valueOrDash(reservation.getLodgingEmail()));
        return message(user, reservation, subject, body, RESERVATION_CANCELLATION);
    }

    private EmailMessage message(User user, ReservationResponse reservation, String subject, String body, String emailType) {
        return new EmailMessage(user.getEmail(), subject, body, emailType, reservation.getId().toString());
    }

    private String confirmationNotesRow(ReservationResponse reservation) {
        String notes = reservation.getNotes();
        if (notes == null || notes.isBlank()) {
            return "";
        }
        return "<tr><td style=\"padding:6px 12px;font-weight:bold;\">Notes</td><td>%s</td></tr>"
                .formatted(escapeHtml(notes.trim()));
    }

    private String valueOrDash(String value) {
        return value == null ? "-" : value;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
