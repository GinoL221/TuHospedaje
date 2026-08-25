package com.tuhospedaje.service;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.User;

public interface EmailOutboxService {

    enum WelcomeResendResult {
        SCHEDULED,
        COOLDOWN
    }

    void enqueueWelcome(User user, RegisterRequest request);

    WelcomeResendResult resendWelcome(User user);

    void enqueueReservationConfirmation(User user, ReservationResponse reservation);

    void enqueueReservationCancellation(User user, ReservationResponse reservation);
}
