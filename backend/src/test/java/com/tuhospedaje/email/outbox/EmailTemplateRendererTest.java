package com.tuhospedaje.email.outbox;

import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.service.EmailTemplateRenderer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();

    @Test
    void rendersConfirmationInSpanishWithLocalFormattedDateTime() {
        ReservationResponse res = reservation(ReservationStatus.CONFIRMED, "Late <arrival> & luggage");
        res.setCreatedAt(LocalDateTime.of(2026, 8, 25, 15, 30, 45));

        EmailMessage message = renderer.renderReservationConfirmation(user(), res);

        assertThat(message.to()).isEqualTo("registered@example.com");
        assertThat(message.emailType()).isEqualTo("RESERVATION_CONFIRMATION");
        assertThat(message.aggregateId()).isEqualTo("42");
        assertThat(message.subject()).isEqualTo("Reserva confirmada — Hotel Sur");
        assertThat(message.htmlBody())
                .contains("¡Tu reserva está confirmada!")
                .contains("Alojamiento", "Hotel Sur — Buenos Aires")
                .contains("Entrada", "20/08/2026")
                .contains("Salida", "22/08/2026")
                .contains("Huésped", "Guest")
                .contains("Número de reserva", "42")
                .contains("Fecha y hora de reserva", "25/08/2026 15:30")
                .contains("Notas", "Late &lt;arrival&gt; &amp; luggage")
                .contains("Total", "$300.00")
                .contains("Estado", "Confirmada")
                .contains("¡Te esperamos!")
                .contains("TuHospedaje &mdash; Tu próxima estadía, confirmada.");
    }

    @Test
    void rendersCancellationInSpanish() {
        EmailMessage message = renderer.renderReservationCancellation(user(), reservation(ReservationStatus.CANCELLED, "   "));

        assertThat(message.to()).isEqualTo("registered@example.com");
        assertThat(message.emailType()).isEqualTo("RESERVATION_CANCELLATION");
        assertThat(message.aggregateId()).isEqualTo("42");
        assertThat(message.subject()).isEqualTo("Reserva cancelada — Hotel Sur");
        assertThat(message.htmlBody())
                .contains("Tu reserva fue cancelada")
                .contains("Tu reserva en Hotel Sur del 20/08/2026 al 22/08/2026 fue cancelada.")
                .contains("Contactate con el alojamiento al 555 o hotel@example.com si necesitás asistencia.");
    }

    @Test
    void escapesAllDynamicFieldsAgainstHtmlInjection() {
        ReservationResponse res = new ReservationResponse();
        res.setId(99L);
        res.setLodgingName("<script>alert(1)</script>");
        res.setCity("<b>City</b>");
        res.setCheckIn(LocalDate.of(2026, 9, 1));
        res.setCheckOut(LocalDate.of(2026, 9, 5));
        res.setGuestName("Attacker \" ' <>&");
        res.setGuestPhone("<tel>");
        res.setTotalPrice(new BigDecimal("100.00"));
        res.setStatus(ReservationStatus.CONFIRMED);
        res.setLodgingPhone("<phone>");
        res.setLodgingEmail("<email>");
        res.setNotes("<xss>payload</xss>");
        res.setCreatedAt(LocalDateTime.of(2026, 8, 25, 10, 0, 0));

        EmailMessage message = renderer.renderReservationConfirmation(user(), res);

        assertThat(message.htmlBody())
                .doesNotContain("<script>", "<b>City</b>", "<tel>", "<phone>", "<email>", "<xss>")
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .contains("&lt;b&gt;City&lt;/b&gt;")
                .contains("Attacker &quot; &#39; &lt;&gt;&amp;")
                .contains("&lt;xss&gt;payload&lt;/xss&gt;");
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
