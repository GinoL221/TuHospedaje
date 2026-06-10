package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailServiceImpl.class);

    @Override
    public void sendWelcomeEmail(RegisterRequest request) {
        log.info("=== EMAIL DE BIENVENIDA (modo consola) ===");
        log.info("Para: {}", request.getEmail());
        log.info("Asunto: Bienvenido a TuHospedaje");
        log.info("Cuerpo: Hola {}, gracias por registrarte.", request.getFirstName());
        log.info("===========================================");
    }

    @Override
    public void sendReservationConfirmation(ReservationResponse reservation) {
        log.info("=== EMAIL DE CONFIRMACIÓN DE RESERVA (modo consola) ===");
        log.info("Para: {}", reservation.getGuestEmail());
        log.info("Asunto: Confirmación de reserva en TuHospedaje");
        log.info("Cuerpo: Hola {}, tu reserva para {} fue confirmada.",
                reservation.getGuestName(), reservation.getLodgingName());
        log.info("Fechas: {} a {}", reservation.getCheckIn(), reservation.getCheckOut());
        log.info("Total: {}", reservation.getTotalPrice());
        log.info("Teléfono huésped: {}", reservation.getGuestPhone());
        log.info("Contacto alojamiento - Teléfono: {}", reservation.getLodgingPhone());
        log.info("Contacto alojamiento - Email: {}", reservation.getLodgingEmail());
        log.info("=======================================================");
    }
}
