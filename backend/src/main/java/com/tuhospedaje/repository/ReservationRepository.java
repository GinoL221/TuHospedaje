package com.tuhospedaje.repository;

import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByLodgingIdAndStatus(Long lodgingId, ReservationStatus status);

    List<Reservation> findByUserIdOrderByCheckInDesc(Long userId);
}