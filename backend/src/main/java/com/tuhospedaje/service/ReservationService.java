package com.tuhospedaje.service;

import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.User;

public interface ReservationService {

    ReservationResponse createReservation(User user, CreateReservationRequest request);

    ReservationResponse getReservationById(Long id);
}
