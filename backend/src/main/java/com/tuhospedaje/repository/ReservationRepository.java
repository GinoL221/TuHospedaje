package com.tuhospedaje.repository;

import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    List<Reservation> findByLodgingIdAndStatus(Long lodgingId, ReservationStatus status);

    /**
     * Acquires a PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) lock on all CONFIRMED reservations
     * for the given lodging. Use this exclusively inside a @Transactional write boundary
     * (createReservation) to serialize concurrent overlap checks for the same lodging.
     *
     * The explicit @Query is required so @Lock reliably emits FOR UPDATE.
     * InnoDB also gap-locks the indexed range on lodging_id, serializing concurrent inserts
     * even when the result set is empty (zero existing reservations).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.lodging.id = :lodgingId AND r.status = :status")
    List<Reservation> lockByLodgingIdAndStatus(@Param("lodgingId") Long lodgingId,
                                               @Param("status") ReservationStatus status);

    List<Reservation> findByUserIdOrderByCheckInDesc(Long userId);

    boolean existsByUserIdAndLodgingIdAndStatus(Long userId, Long lodgingId, ReservationStatus status);

    List<Reservation> findAllByOrderByIdDesc();

    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);
}