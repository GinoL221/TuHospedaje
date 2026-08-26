package com.tuhospedaje.email.outbox;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.EmailOutboxStatus;
import com.tuhospedaje.enums.EmailOutboxType;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.service.WelcomeEmailRenderer;
import com.tuhospedaje.service.impl.EmailOutboxServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOutboxServiceImplTest {

    @Mock
    private EmailOutboxRepository repository;
    @Mock
    private WelcomeEmailRenderer welcomeEmailRenderer;

    @Test
    void enqueueWelcomeStoresTheRenderedWelcomeMessage() {
        User user = user(7L, "ana@example.com");
        RegisterRequest request = new RegisterRequest("Ana", "Gomez", "ana@example.com", "secret");
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7")).thenReturn(Optional.empty());

        when(welcomeEmailRenderer.render(7L, "ana@example.com", "Ana"))
                .thenReturn(new EmailMessage("ana@example.com", "¡Bienvenido a TuHospedaje!", "<p>Hola Ana</p>", "WELCOME", "7"));

        newService().enqueueWelcome(user, request);

        EmailOutbox saved = capturedFlushedOutbox();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getEmailType()).isEqualTo(EmailOutboxType.WELCOME.name());
        assertThat(saved.getAggregateId()).isEqualTo("7");
        assertThat(saved.getRecipient()).isEqualTo("ana@example.com");
        assertThat(saved.getSubject()).isEqualTo("¡Bienvenido a TuHospedaje!");
        assertThat(saved.getHtmlBody()).isEqualTo("<p>Hola Ana</p>");
        assertThat(saved.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
    }

    @Test
    void enqueueReservationConfirmationStoresTheRenderedConfirmationMessage() {
        User user = user(8L, "registered@example.com");
        ReservationResponse reservation = reservation(42L, "guest@example.com", ReservationStatus.CONFIRMED);
        when(repository.findByEmailTypeAndAggregateId("RESERVATION_CONFIRMATION", "42"))
                .thenReturn(Optional.empty());

        newService().enqueueReservationConfirmation(user, reservation);

        EmailOutbox saved = capturedOutbox();
        assertThat(saved.getEmailType()).isEqualTo(EmailOutboxType.RESERVATION_CONFIRMATION.name());
        assertThat(saved.getAggregateId()).isEqualTo("42");
        assertThat(saved.getRecipient()).isEqualTo("registered@example.com");
        assertThat(saved.getSubject()).isEqualTo("Booking confirmed — Hotel Sur");
        assertThat(saved.getHtmlBody()).contains("Your booking is confirmed!")
                .contains("Hotel Sur")
                .contains("Reservation number")
                .contains("42")
                .contains("hotel@example.com");
    }

    @Test
    void enqueueReservationConfirmationEscapesNonEmptyNotes() {
        User user = user(8L, "registered@example.com");
        ReservationResponse reservation = reservation(42L, "guest@example.com", ReservationStatus.CONFIRMED);
        reservation.setNotes("Late <arrival> & luggage");
        when(repository.findByEmailTypeAndAggregateId("RESERVATION_CONFIRMATION", "42"))
                .thenReturn(Optional.empty());

        newService().enqueueReservationConfirmation(user, reservation);

        assertThat(capturedOutbox().getHtmlBody())
                .contains("Notes")
                .contains("Late &lt;arrival&gt; &amp; luggage");
    }

    @Test
    void enqueueReservationConfirmationOmitsBlankNotes() {
        User user = user(8L, "registered@example.com");
        ReservationResponse reservation = reservation(42L, "guest@example.com", ReservationStatus.CONFIRMED);
        reservation.setNotes("   ");
        when(repository.findByEmailTypeAndAggregateId("RESERVATION_CONFIRMATION", "42"))
                .thenReturn(Optional.empty());

        newService().enqueueReservationConfirmation(user, reservation);

        assertThat(capturedOutbox().getHtmlBody()).doesNotContain("Notes");
    }

    @Test
    void enqueueReservationCancellationStoresTheRenderedCancellationMessage() {
        User user = user(8L, "registered@example.com");
        ReservationResponse reservation = reservation(42L, "guest@example.com", ReservationStatus.CANCELLED);
        when(repository.findByEmailTypeAndAggregateId("RESERVATION_CANCELLATION", "42"))
                .thenReturn(Optional.empty());

        newService().enqueueReservationCancellation(user, reservation);

        EmailOutbox saved = capturedOutbox();
        assertThat(saved.getEmailType()).isEqualTo(EmailOutboxType.RESERVATION_CANCELLATION.name());
        assertThat(saved.getAggregateId()).isEqualTo("42");
        assertThat(saved.getRecipient()).isEqualTo("registered@example.com");
        assertThat(saved.getSubject()).isEqualTo("Booking cancelled — Hotel Sur");
        assertThat(saved.getHtmlBody()).contains("Your booking was cancelled")
                .contains("2026-08-20")
                .contains("2026-08-22")
                .contains("555")
                .contains("hotel@example.com");
    }

    @Test
    void duplicateAggregateAndTypeDoesNotCreateAnotherOutboxRow() {
        User user = user(7L, "ana@example.com");
        RegisterRequest request = new RegisterRequest("Ana", "Gomez", "ana@example.com", "secret");
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7"))
                .thenReturn(Optional.of(new EmailOutbox()));
        when(welcomeEmailRenderer.render(7L, "ana@example.com", "Ana"))
                .thenReturn(new EmailMessage("ana@example.com", "¡Bienvenido a TuHospedaje!", "<p>Hola Ana</p>", "WELCOME", "7"));

        newService().enqueueWelcome(user, request);

        verify(repository, never()).save(any(EmailOutbox.class));
    }

    private EmailOutbox capturedOutbox() {
        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private EmailOutbox capturedFlushedOutbox() {
        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(repository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private EmailOutboxServiceImpl newService() {
        return new EmailOutboxServiceImpl(repository, welcomeEmailRenderer);
    }

    private static User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    private static ReservationResponse reservation(Long id, String email, ReservationStatus status) {
        ReservationResponse reservation = new ReservationResponse();
        reservation.setId(id);
        reservation.setLodgingName("Hotel Sur");
        reservation.setCity("Buenos Aires");
        reservation.setCheckIn(LocalDate.of(2026, 8, 20));
        reservation.setCheckOut(LocalDate.of(2026, 8, 22));
        reservation.setGuestName("Guest");
        reservation.setGuestEmail(email);
        reservation.setGuestPhone("123");
        reservation.setTotalPrice(new BigDecimal("300.00"));
        reservation.setStatus(status);
        reservation.setLodgingPhone("555");
        reservation.setLodgingEmail("hotel@example.com");
        return reservation;
    }
}
