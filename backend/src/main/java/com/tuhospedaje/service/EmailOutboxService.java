package com.tuhospedaje.service;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.User;

public interface EmailOutboxService {

    void enqueueWelcome(User user, RegisterRequest request);

    void enqueueReservationConfirmation(User user, ReservationResponse reservation);

    void enqueueReservationCancellation(User user, ReservationResponse reservation);
}
