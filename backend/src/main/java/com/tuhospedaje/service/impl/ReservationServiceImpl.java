package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.common.PageResponse;
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
import com.tuhospedaje.repository.specification.ReservationSpecifications;
import com.tuhospedaje.service.EmailService;
import com.tuhospedaje.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.Set;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    private final ReservationRepository reservationRepository;
    private final LodgingRepository lodgingRepository;
    private final EmailService emailService;
    private final Clock clock;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  LodgingRepository lodgingRepository, EmailService emailService,
                                  Clock clock) {
        this.reservationRepository = reservationRepository;
        this.lodgingRepository = lodgingRepository;
        this.emailService = emailService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReservationResponse cancelReservation(Long id, User requester) {
        Reservation reservation = reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> reservationNotFound(id));
        if (!reservation.getUser().getId().equals(requester.getId())) {
            throw reservationNotFound(id);
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            log.info("reservation.cancel.idempotent reservationId={}", id);
            return ReservationResponse.fromEntity(reservation);
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("La reserva no se puede cancelar");
        }
        if (!LocalDate.now(clock).isBefore(reservation.getCheckIn())) {
            log.info("reservation.cancel.rejected_deadline reservationId={}", id);
            throw new IllegalArgumentException("La reserva ya no se puede cancelar");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        ReservationResponse response = ReservationResponse.fromEntity(reservation);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.sendReservationCancellation(response);
                } catch (RuntimeException ex) {
                    log.warn("reservation.cancel.email_failed reservationId={}", id);
                }
            }
        });
        log.info("reservation.cancelled reservationId={}", id);
        return response;
    }

    private ResourceNotFoundException reservationNotFound(Long id) {
        return new ResourceNotFoundException("Reserva no encontrada con ID: " + id);
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

    private static final Set<String> ADMIN_SORT_FIELDS = Set.of(
            "id", "checkIn", "checkOut", "status", "totalPrice"
    );

    private ReservationStatus parseReservationStatus(String status) {
        return ReservationStatus.valueOf(status.toUpperCase(Locale.ROOT));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getAdminReservations(int page, int size, String sort, String direction, String status, String q) {
        if (!ADMIN_SORT_FIELDS.contains(sort)) {
            throw new IllegalArgumentException("Campo de ordenamiento inválido: " + sort);
        }

        Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new IllegalArgumentException("Dirección de ordenamiento inválida: " + direction));
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<Reservation> reservationPage;

        if (status != null && q != null && !q.trim().isEmpty()) {
            String searchTerm = q.trim();
            reservationPage = reservationRepository.findAll(
                    ReservationSpecifications.withStatusAndSearchQuery(parseReservationStatus(status), searchTerm),
                    pageable);
        } else if (status != null) {
            reservationPage = reservationRepository.findByStatus(parseReservationStatus(status), pageable);
        } else if (q != null && !q.trim().isEmpty()) {
            String searchTerm = q.trim();
            reservationPage = reservationRepository.findAll(
                    ReservationSpecifications.withSearchQuery(searchTerm),
                    pageable);
        } else {
            reservationPage = reservationRepository.findAll(pageable);
        }

        List<ReservationResponse> items = reservationPage.getContent().stream()
                .map(ReservationResponse::fromEntity)
                .toList();

        return new PageResponse<>(
                items,
                reservationPage.getNumber(),
                reservationPage.getTotalElements(),
                reservationPage.getTotalPages()
        );
    }
}
