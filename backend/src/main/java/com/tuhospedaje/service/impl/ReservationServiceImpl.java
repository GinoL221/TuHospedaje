package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.service.EmailService;
import com.tuhospedaje.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final LodgingRepository lodgingRepository;
    private final EmailService emailService;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  LodgingRepository lodgingRepository, EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.lodgingRepository = lodgingRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public ReservationResponse createReservation(User user, CreateReservationRequest request) {
        Lodging lodging = lodgingRepository.findById(request.getLodgingId())
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado"));

        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal totalPrice = lodging.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        // Use the pessimistic-write locking query to acquire FOR UPDATE on the lodging's
        // CONFIRMED reservations. This serializes concurrent overlap checks for the same lodging.
        // Read paths (search, checkAvailability) continue to use the non-locking findByLodgingIdAndStatus.
        boolean hasOverlap = reservationRepository
                .lockByLodgingIdAndStatus(request.getLodgingId(), ReservationStatus.CONFIRMED)
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
        reservation.setGuestPhone(request.getGuestPhone());
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.save(reservation);
        ReservationResponse response = ReservationResponse.fromEntity(saved);
        emailService.sendReservationConfirmation(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, User requester) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));
        boolean isOwner = reservation.getUser().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == RoleEnum.ADMIN;
        if (!isOwner && !isAdmin) {
            // 404 instead of 403 to hide resource existence (IDOR prevention)
            throw new ResourceNotFoundException("Reserva no encontrada con ID: " + id);
        }
        return ReservationResponse.fromEntity(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(User user) {
        return reservationRepository.findByUserIdOrderByCheckInDesc(user.getId())
                .stream()
                .map(ReservationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAllByOrderByIdDesc()
                .stream()
                .map(ReservationResponse::fromEntity)
                .toList();
    }
}
