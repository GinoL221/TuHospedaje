package com.tuhospedaje.dto.reservation;

import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ReservationResponse {

    private Long id;
    private Long lodgingId;
    private String lodgingName;
    private String city;
    private Long userId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private BigDecimal totalPrice;
    private ReservationStatus status;
    private Long version;

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
        return dto;
    }
}
