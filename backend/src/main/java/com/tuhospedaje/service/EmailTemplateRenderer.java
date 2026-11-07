package com.tuhospedaje.service;

import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailTemplateRenderer {

    private static final String RESERVATION_CONFIRMATION = "RESERVATION_CONFIRMATION";
    private static final String RESERVATION_CANCELLATION = "RESERVATION_CANCELLATION";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public EmailMessage renderReservationConfirmation(User user, ReservationResponse reservation) {
        String subject = "Reserva confirmada — " + reservation.getLodgingName();
        String body = """
                <html><body style="font-family:sans-serif;color:#222;">
                <h2 style="color:#c0392b;">¡Tu reserva está confirmada!</h2>
                <table style="border-collapse:collapse;width:100%%">
                  <tr><td style="padding:6px 12px;font-weight:bold;">Alojamiento</td><td>%s — %s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Entrada</td><td>%s</td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Salida</td><td>%s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Huésped</td><td>%s</td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Número de reserva</td><td>%s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Fecha y hora de reserva</td><td>%s</td></tr>
                  %s
                  <tr><td style="padding:6px 12px;font-weight:bold;">Teléfono</td><td>%s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Total</td><td><strong>$%s</strong></td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Estado</td><td>%s</td></tr>
                  <tr style="background:#f9f9f9"><td style="padding:6px 12px;font-weight:bold;">Teléfono del alojamiento</td><td>%s</td></tr>
                  <tr><td style="padding:6px 12px;font-weight:bold;">Email del alojamiento</td><td>%s</td></tr>
                </table>
                <p style="margin-top:20px;">¡Te esperamos!</p>
                <hr><p style="font-size:12px;color:#888;">TuHospedaje &mdash; Tu próxima estadía, confirmada.</p>
                </body></html>
                """.formatted(
                escapeHtml(reservation.getLodgingName()), escapeHtml(reservation.getCity()),
                formatDate(reservation.getCheckIn()), formatDate(reservation.getCheckOut()),
                escapeHtml(reservation.getGuestName()), reservation.getId(),
                formatDateTime(reservation.getCreatedAt()), confirmationNotesRow(reservation),
                escapeHtml(valueOrDash(reservation.getGuestPhone())), reservation.getTotalPrice(),
                formatStatus(reservation.getStatus()),
                escapeHtml(valueOrDash(reservation.getLodgingPhone())), escapeHtml(valueOrDash(reservation.getLodgingEmail())));
        return message(user, reservation, subject, body, RESERVATION_CONFIRMATION);
    }

    public EmailMessage renderReservationCancellation(User user, ReservationResponse reservation) {
        String subject = "Reserva cancelada — " + reservation.getLodgingName();
        String body = """
                <html><body style="font-family:sans-serif;color:#222;">
                <h2>Tu reserva fue cancelada</h2>
                <p>Tu reserva en %s del %s al %s fue cancelada.</p>
                <p>Contactate con el alojamiento al %s o %s si necesitás asistencia.</p>
                <hr><p style="font-size:12px;color:#888;">TuHospedaje</p>
                </body></html>
                """.formatted(
                escapeHtml(reservation.getLodgingName()),
                formatDate(reservation.getCheckIn()),
                formatDate(reservation.getCheckOut()),
                escapeHtml(valueOrDash(reservation.getLodgingPhone())),
                escapeHtml(valueOrDash(reservation.getLodgingEmail())));
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
        return "<tr><td style=\"padding:6px 12px;font-weight:bold;\">Notas</td><td>%s</td></tr>"
                .formatted(escapeHtml(notes.trim()));
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String formatStatus(ReservationStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case CONFIRMED -> "Confirmada";
            case CANCELLED -> "Cancelada";
        };
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
