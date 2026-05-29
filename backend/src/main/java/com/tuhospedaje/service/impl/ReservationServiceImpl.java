package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.service.ReservationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final LodgingRepository lodgingRepository;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  LodgingRepository lodgingRepository) {
        this.reservationRepository = reservationRepository;
        this.lodgingRepository = lodgingRepository;
    }

    @Override
    public ReservationResponse createReservation(User user, CreateReservationRequest request) {
        Lodging lodging = lodgingRepository.findById(request.getLodgingId())
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado"));

        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal totalPrice = lodging.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        boolean hasOverlap = reservationRepository
                .findByLodgingIdAndStatus(request.getLodgingId(), ReservationStatus.CONFIRMED)
                .stream()
                .anyMatch(r -> r.getCheckIn().isBefore(request.getCheckOut())
                        && r.getCheckOut().isAfter(request.getCheckIn()));

        if (hasOverlap) {
            throw new IllegalArgumentException("El alojamiento no está disponible para las fechas seleccionadas");
        }

        Reservation reservation = new Reservation();
        reservation.setLodging(lodging);
        reservation.setUser(user);
        reservation.setCheckIn(request.getCheckIn());
        reservation.setCheckOut(request.getCheckOut());
        reservation.setGuestName(request.getGuestName());
        reservation.setGuestEmail(request.getGuestEmail());
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(saved);
    }

    @Override
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));
        return ReservationResponse.fromEntity(reservation);
    }
}
