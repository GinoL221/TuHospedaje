package com.tuhospedaje.dto.reservation;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateReservationRequest {

    @NotNull(message = "El ID del alojamiento es obligatorio")
    private Long lodgingId;

    @NotNull(message = "La fecha de check-in es obligatoria")
    @FutureOrPresent(message = "La fecha de check-in no puede ser pasada")
    private LocalDate checkIn;

    @NotNull(message = "La fecha de check-out es obligatoria")
    @FutureOrPresent(message = "La fecha de check-out no puede ser pasada")
    private LocalDate checkOut;

    @NotBlank(message = "El nombre del huésped es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String guestName;

    @NotBlank(message = "El email del huésped es obligatorio")
    @Email(message = "Formato de email inválido")
    private String guestEmail;

    @AssertTrue(message = "La fecha de check-out debe ser posterior al check-in")
    private boolean isCheckOutAfterCheckIn() {
        if (checkIn == null || checkOut == null) return false;
        return checkOut.isAfter(checkIn);
    }
}
