package com.tuhospedaje.reservation;

import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.service.EmailService;
import com.tuhospedaje.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationCancellationServiceTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    @Mock ReservationRepository reservationRepository;
    @Mock LodgingRepository lodgingRepository;
    @Mock EmailService emailService;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void ownerCanCancelUntilEndOfPreviousBusinessDayAndMailRunsAfterCommit() {
        var reservation = reservation(1L, 7L, ReservationStatus.CONFIRMED);
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));
        TransactionSynchronizationManager.initSynchronization();

        var result = serviceAt("2026-08-20T02:59:59Z").cancelReservation(1L, user(7L));

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(emailService, never()).sendReservationCancellation(result);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(emailService).sendReservationCancellation(result);
    }

    @Test
    void checkInBusinessDateIsRejectedAndStateIsUnchanged() {
        var reservation = reservation(1L, 7L, ReservationStatus.CONFIRMED);
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> serviceAt("2026-08-20T03:00:00Z").cancelReservation(1L, user(7L)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(emailService, never()).sendReservationCancellation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingAndNonOwnerUseTheSameNotFoundSemantics() {
        when(reservationRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        when(reservationRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(reservation(1L, 7L, ReservationStatus.CONFIRMED)));

        var service = serviceAt("2026-08-19T12:00:00Z");
        assertThatThrownBy(() -> service.cancelReservation(99L, user(8L)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.cancelReservation(1L, user(8L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void repeatedCancellationIsIdempotentWithoutAnotherMail() {
        var reservation = reservation(1L, 7L, ReservationStatus.CANCELLED);
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        var result = serviceAt("2026-08-19T12:00:00Z").cancelReservation(1L, user(7L));

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(emailService, never()).sendReservationCancellation(result);
    }

    @Test
    void mailFailureAfterCommitCannotUndoCancellation() {
        var reservation = reservation(1L, 7L, ReservationStatus.CONFIRMED);
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp down"))
                .when(emailService).sendReservationCancellation(org.mockito.ArgumentMatchers.any());
        TransactionSynchronizationManager.initSynchronization();

        var result = serviceAt("2026-08-19T12:00:00Z").cancelReservation(1L, user(7L));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(emailService, times(1)).sendReservationCancellation(result);
    }

    private ReservationServiceImpl serviceAt(String instant) {
        return new ReservationServiceImpl(reservationRepository, lodgingRepository, emailService,
                Clock.fixed(Instant.parse(instant), BUSINESS_ZONE));
    }

    private static User user(Long id) {
        var user = new User();
        user.setId(id);
        user.setRole(RoleEnum.USER);
        return user;
    }

    private static Reservation reservation(Long id, Long ownerId, ReservationStatus status) {
        var lodging = new Lodging();
        lodging.setId(2L);
        lodging.setName("Test lodging");
        lodging.setCity("Buenos Aires");
        var reservation = new Reservation();
        reservation.setId(id);
        reservation.setUser(user(ownerId));
        reservation.setLodging(lodging);
        reservation.setCheckIn(java.time.LocalDate.of(2026, 8, 20));
        reservation.setCheckOut(java.time.LocalDate.of(2026, 8, 22));
        reservation.setGuestName("Guest");
        reservation.setGuestEmail("guest@example.com");
        reservation.setGuestPhone("123");
        reservation.setTotalPrice(BigDecimal.TEN);
        reservation.setStatus(status);
        return reservation;
    }
}
