package com.tuhospedaje.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Payload to create a new reservation")
public class CreateReservationRequest {

    @NotNull(message = "El ID del alojamiento es obligatorio")
    @Schema(description = "ID of the lodging to reserve", example = "42")
    private Long lodgingId;

    @NotNull(message = "La fecha de check-in es obligatoria")
    @FutureOrPresent(message = "La fecha de check-in no puede ser pasada")
    @Schema(description = "Check-in date (ISO 8601)", example = "2025-07-15")
    private LocalDate checkIn;

    @NotNull(message = "La fecha de check-out es obligatoria")
    @FutureOrPresent(message = "La fecha de check-out no puede ser pasada")
    @Schema(description = "Check-out date (ISO 8601) — must be after checkIn", example = "2025-07-20")
    private LocalDate checkOut;

    @NotBlank(message = "El nombre del huésped es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Schema(description = "Full name of the guest", example = "Maria Gomez")
    private String guestName;

    @NotBlank(message = "El email del huésped es obligatorio")
    @Email(message = "Formato de email inválido")
    @Schema(description = "Email address of the guest", example = "maria.gomez@example.com")
    private String guestEmail;

    @NotBlank(message = "El teléfono del huésped es obligatorio")
    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    @Schema(description = "Phone number of the guest", example = "+5491112345678")
    private String guestPhone;

    @AssertTrue(message = "La fecha de check-out debe ser posterior al check-in")
    private boolean isCheckOutAfterCheckIn() {
        if (checkIn == null || checkOut == null) return false;
        return checkOut.isAfter(checkIn);
    }
}
