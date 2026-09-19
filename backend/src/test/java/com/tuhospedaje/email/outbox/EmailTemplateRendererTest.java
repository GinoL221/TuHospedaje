package com.tuhospedaje.email.outbox;

import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.service.EmailTemplateRenderer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();

    @Test
    void rendersConfirmationUsingTheCurrentOutboxCopy() {
        EmailMessage message = renderer.renderReservationConfirmation(user(), reservation(ReservationStatus.CONFIRMED, "Late <arrival> & luggage"));

        assertThat(message.to()).isEqualTo("registered@example.com");
        assertThat(message.emailType()).isEqualTo("RESERVATION_CONFIRMATION");
        assertThat(message.aggregateId()).isEqualTo("42");
        assertThat(message.subject()).isEqualTo("Booking confirmed — Hotel Sur");
        assertThat(message.htmlBody()).contains("Your booking is confirmed!", "Hotel Sur", "Reservation number", "42")
                .contains("Late &lt;arrival&gt; &amp; luggage", "hotel@example.com");
    }

    @Test
    void rendersCancellationUsingTheCurrentOutboxCopy() {
        EmailMessage message = renderer.renderReservationCancellation(user(), reservation(ReservationStatus.CANCELLED, "   "));

        assertThat(message.to()).isEqualTo("registered@example.com");
        assertThat(message.emailType()).isEqualTo("RESERVATION_CANCELLATION");
        assertThat(message.aggregateId()).isEqualTo("42");
        assertThat(message.subject()).isEqualTo("Booking cancelled — Hotel Sur");
        assertThat(message.htmlBody()).contains("Your booking was cancelled", "2026-08-20", "2026-08-22", "555", "hotel@example.com");
    }

    private static User user() {
        User user = new User();
        user.setEmail("registered@example.com");
        return user;
    }

    private static ReservationResponse reservation(ReservationStatus status, String notes) {
        ReservationResponse reservation = new ReservationResponse();
        reservation.setId(42L);
        reservation.setLodgingName("Hotel Sur");
        reservation.setCity("Buenos Aires");
        reservation.setCheckIn(LocalDate.of(2026, 8, 20));
        reservation.setCheckOut(LocalDate.of(2026, 8, 22));
        reservation.setGuestName("Guest");
        reservation.setGuestPhone("123");
        reservation.setTotalPrice(new BigDecimal("300.00"));
        reservation.setStatus(status);
        reservation.setLodgingPhone("555");
        reservation.setLodgingEmail("hotel@example.com");
        reservation.setNotes(notes);
        return reservation;
    }
}
