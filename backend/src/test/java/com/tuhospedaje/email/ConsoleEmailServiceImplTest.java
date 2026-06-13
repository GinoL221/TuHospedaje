package com.tuhospedaje.email;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.service.impl.ConsoleEmailServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatNoException;

class ConsoleEmailServiceImplTest {

    private final ConsoleEmailServiceImpl service = new ConsoleEmailServiceImpl();

    @Test
    void sendWelcomeEmail_doesNotThrow() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Ana")
                .lastName("García")
                .email("ana@test.com")
                .password("secret123")
                .build();

        assertThatNoException().isThrownBy(() -> service.sendWelcomeEmail(request));
    }

    @Test
    void sendReservationConfirmation_doesNotThrow() {
        ReservationResponse reservation = new ReservationResponse();
        reservation.setGuestEmail("guest@test.com");
        reservation.setGuestName("Carlos López");
        reservation.setLodgingName("Hotel Sur");
        reservation.setCheckIn(LocalDate.of(2026, 7, 10));
        reservation.setCheckOut(LocalDate.of(2026, 7, 14));
        reservation.setTotalPrice(new BigDecimal("400.00"));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setGuestPhone("111222333");
        reservation.setLodgingPhone("444555666");
        reservation.setLodgingEmail("hotel@sur.com");

        assertThatNoException().isThrownBy(() -> service.sendReservationConfirmation(reservation));
    }
}
