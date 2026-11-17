package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.common.PageResponse;
import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.exception.LocalizedIllegalArgumentException;
import com.tuhospedaje.exception.ReservationConflictException;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.repository.specification.ReservationSpecifications;
import com.tuhospedaje.service.AuthenticatedActor;
import com.tuhospedaje.service.EmailOutboxService;
import com.tuhospedaje.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Set;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    private final ReservationRepository reservationRepository;
    private final LodgingRepository lodgingRepository;
    private final UserRepository userRepository;
    private final EmailOutboxService emailOutboxService;
    private final Clock clock;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  LodgingRepository lodgingRepository, UserRepository userRepository,
                                  EmailOutboxService emailOutboxService, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.lodgingRepository = lodgingRepository;
        this.userRepository = userRepository;
        this.emailOutboxService = emailOutboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReservationResponse cancelReservation(Long id, AuthenticatedActor requester) {
        Reservation reservation = reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> reservationNotFound(id));
        if (!reservation.getUser().getId().equals(requester.id())) {
            throw reservationNotFound(id);
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            log.info("reservation.cancel.idempotent reservationId={}", id);
            return ReservationResponse.fromEntity(reservation);
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new LocalizedIllegalArgumentException("error.reservation.cannot_cancel");
        }
        if (!LocalDate.now(clock).isBefore(reservation.getCheckIn())) {
            log.info("reservation.cancel.rejected_deadline reservationId={}", id);
            throw new LocalizedIllegalArgumentException("error.reservation.cancel_deadline_passed");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        ReservationResponse response = ReservationResponse.fromEntity(reservation);
        emailOutboxService.enqueueReservationCancellation(reservation.getUser(), response);
        log.info("reservation.cancelled reservationId={}", id);
        return response;
    }

    private ResourceNotFoundException reservationNotFound(Long id) {
        return new ResourceNotFoundException(
                "error.reservation.not_found",
                new Object[]{String.valueOf(id)},
                "Reservation not found with ID: " + id);
    }

    @Override
    @Transactional
    public ReservationResponse createReservation(AuthenticatedActor actor, CreateReservationRequest request) {
        // Acquire PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) on the parent Lodging row.
        // This serializes concurrent reservation attempts for the same lodging, ensuring
        // atomicity and preventing double booking even when there are zero existing reservations
        // (where reservation-level locks would match no rows).
        Lodging lodging = lodgingRepository.findByIdForUpdate(request.getLodgingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "error.reservation.lodging_not_found",
                        new Object[0],
                        "Lodging not found"));

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
            throw new ReservationConflictException("error.reservation.not_available");
        }

        User user = userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "error.user.not_found",
                        new Object[0],
                        "User not found"));
        Reservation reservation = new Reservation();
        reservation.setLodging(lodging);
        reservation.setUser(user);
        reservation.setCheckIn(request.getCheckIn());
        reservation.setCheckOut(request.getCheckOut());
        reservation.setGuestName(actor.firstName() + " " + actor.lastName());
        reservation.setGuestEmail(actor.email());
        reservation.setGuestPhone(request.getGuestPhone());
        reservation.setNotes(normalizeNotes(request.getNotes()));
        reservation.setCreatedAt(LocalDateTime.now(clock));
        reservation.setCreatedAtDerived(false);
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.save(reservation);
        ReservationResponse response = ReservationResponse.fromEntity(saved);
        emailOutboxService.enqueueReservationConfirmation(user, response);
        return response;
    }

    private static String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String normalizedNotes = notes.trim();
        return normalizedNotes.isEmpty() ? null : normalizedNotes;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, AuthenticatedActor requester) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> reservationNotFound(id));
        boolean isOwner = reservation.getUser().getId().equals(requester.id());
        boolean isAdmin = requester.role() == RoleEnum.ADMIN;
        if (!isOwner && !isAdmin) {
            // 404 instead of 403 to hide resource existence (IDOR prevention)
            throw reservationNotFound(id);
        }
        return ReservationResponse.fromEntity(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(AuthenticatedActor actor) {
        return reservationRepository.findByUserIdOrderByCheckInDesc(actor.id())
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
            throw new LocalizedIllegalArgumentException("error.sort.invalid_field", sort);
        }

        Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new LocalizedIllegalArgumentException("error.sort.invalid_direction", direction));
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
