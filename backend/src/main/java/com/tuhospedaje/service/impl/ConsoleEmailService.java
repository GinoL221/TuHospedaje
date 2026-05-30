package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void sendWelcomeEmail(RegisterRequest request) {
        log.info("=== EMAIL DE BIENVENIDA (modo consola) ===");
        log.info("Para: {}", request.getEmail());
        log.info("Asunto: Bienvenido a TuHospedaje");
        log.info("Cuerpo: Hola {}, gracias por registrarte.", request.getFirstName());
        log.info("===========================================");
    }
}
