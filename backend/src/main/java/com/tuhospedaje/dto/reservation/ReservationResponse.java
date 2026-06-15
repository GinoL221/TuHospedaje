package com.tuhospedaje.dto.reservation;

import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.enums.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Reservation details returned after creation or retrieval")
public class ReservationResponse {

    @Schema(description = "Unique reservation ID", example = "101")
    private Long id;

    @Schema(description = "ID of the reserved lodging", example = "42")
    private Long lodgingId;

    @Schema(description = "Name of the reserved lodging", example = "Casa del Sol")
    private String lodgingName;

    @Schema(description = "City where the lodging is located", example = "Buenos Aires")
    private String city;

    @Schema(description = "ID of the user who created the reservation", example = "7")
    private Long userId;

    @Schema(description = "Check-in date", example = "2025-07-15")
    private LocalDate checkIn;

    @Schema(description = "Check-out date", example = "2025-07-20")
    private LocalDate checkOut;

    @Schema(description = "Full name of the guest", example = "Maria Gomez")
    private String guestName;

    @Schema(description = "Email address of the guest", example = "maria.gomez@example.com")
    private String guestEmail;

    @Schema(description = "Phone number of the guest", example = "+5491112345678")
    private String guestPhone;

    @Schema(description = "Total price for the stay", example = "7500.00")
    private BigDecimal totalPrice;

    @Schema(description = "Current status of the reservation", example = "CONFIRMED")
    private ReservationStatus status;

    @Schema(description = "Optimistic lock version — used internally to detect concurrent updates", example = "0")
    private Long version;

    @Schema(description = "Contact phone number of the lodging", example = "+54114567890")
    private String lodgingPhone;

    @Schema(description = "Contact email of the lodging", example = "contacto@casadelsol.com")
    private String lodgingEmail;

    public static ReservationResponse fromEntity(Reservation reservation) {
        ReservationResponse dto = new ReservationResponse();
        dto.setId(reservation.getId());
        dto.setLodgingId(reservation.getLodging().getId());
        dto.setLodgingName(reservation.getLodging().getName());
        dto.setCity(reservation.getLodging().getCity());
        dto.setUserId(reservation.getUser().getId());
        dto.setCheckIn(reservation.getCheckIn());
        dto.setCheckOut(reservation.getCheckOut());
        dto.setGuestName(reservation.getGuestName());
        dto.setGuestEmail(reservation.getGuestEmail());
        dto.setGuestPhone(reservation.getGuestPhone());
        dto.setTotalPrice(reservation.getTotalPrice());
        dto.setStatus(reservation.getStatus());
        dto.setVersion(reservation.getVersion());
        dto.setLodgingPhone(reservation.getLodging().getPhoneNumber());
        dto.setLodgingEmail(reservation.getLodging().getEmail());
        return dto;
    }
}
