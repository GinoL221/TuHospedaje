package com.tuhospedaje.service;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;

public interface EmailService {

    void sendWelcomeEmail(RegisterRequest request);

    void sendReservationConfirmation(ReservationResponse reservation);
}
