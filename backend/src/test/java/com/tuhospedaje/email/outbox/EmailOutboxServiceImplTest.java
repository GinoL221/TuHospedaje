package com.tuhospedaje.email.outbox;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.EmailOutboxStatus;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.repository.EmailOutboxRepository;
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

    @Test
    void enqueueWelcomeStoresTheRenderedWelcomeMessage() {
        User user = user(7L, "ana@example.com");
        RegisterRequest request = new RegisterRequest("Ana", "Gomez", "ana@example.com", "secret");
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7")).thenReturn(Optional.empty());

        new EmailOutboxServiceImpl(repository).enqueueWelcome(user, request);

        EmailOutbox saved = capturedOutbox();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getEmailType()).isEqualTo("WELCOME");
        assertThat(saved.getAggregateId()).isEqualTo("7");
        assertThat(saved.getRecipient()).isEqualTo("ana@example.com");
        assertThat(saved.getSubject()).isEqualTo("Welcome to TuHospedaje!");
        assertThat(saved.getHtmlBody()).contains("Welcome to TuHospedaje, Ana!");
        assertThat(saved.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
    }

    @Test
    void enqueueReservationConfirmationStoresTheRenderedConfirmationMessage() {
        User user = user(8L, "guest@example.com");
        ReservationResponse reservation = reservation(42L, "guest@example.com", ReservationStatus.CONFIRMED);
        when(repository.findByEmailTypeAndAggregateId("RESERVATION_CONFIRMATION", "42"))
                .thenReturn(Optional.empty());

        new EmailOutboxServiceImpl(repository).enqueueReservationConfirmation(user, reservation);

        EmailOutbox saved = capturedOutbox();
        assertThat(saved.getEmailType()).isEqualTo("RESERVATION_CONFIRMATION");
        assertThat(saved.getAggregateId()).isEqualTo("42");
        assertThat(saved.getRecipient()).isEqualTo("guest@example.com");
        assertThat(saved.getSubject()).isEqualTo("Booking confirmed — Hotel Sur");
        assertThat(saved.getHtmlBody()).contains("Your booking is confirmed!")
                .contains("Hotel Sur")
                .contains("hotel@example.com");
    }

    @Test
    void enqueueReservationCancellationStoresTheRenderedCancellationMessage() {
        User user = user(8L, "guest@example.com");
        ReservationResponse reservation = reservation(42L, "guest@example.com", ReservationStatus.CANCELLED);
        when(repository.findByEmailTypeAndAggregateId("RESERVATION_CANCELLATION", "42"))
                .thenReturn(Optional.empty());

        new EmailOutboxServiceImpl(repository).enqueueReservationCancellation(user, reservation);

        EmailOutbox saved = capturedOutbox();
        assertThat(saved.getEmailType()).isEqualTo("RESERVATION_CANCELLATION");
        assertThat(saved.getAggregateId()).isEqualTo("42");
        assertThat(saved.getRecipient()).isEqualTo("guest@example.com");
        assertThat(saved.getSubject()).isEqualTo("Booking cancelled — Hotel Sur");
        assertThat(saved.getHtmlBody()).contains("Your booking was cancelled")
                .contains("2026-08-20")
                .contains("2026-08-22");
    }

    @Test
    void duplicateAggregateAndTypeDoesNotCreateAnotherOutboxRow() {
        User user = user(7L, "ana@example.com");
        RegisterRequest request = new RegisterRequest("Ana", "Gomez", "ana@example.com", "secret");
        when(repository.findByEmailTypeAndAggregateId("WELCOME", "7"))
                .thenReturn(Optional.of(new EmailOutbox()));

        new EmailOutboxServiceImpl(repository).enqueueWelcome(user, request);

        verify(repository, never()).save(any(EmailOutbox.class));
    }

    private EmailOutbox capturedOutbox() {
        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
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
