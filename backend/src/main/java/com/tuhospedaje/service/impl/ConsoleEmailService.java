package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);
    private final JavaMailSender mailSender;

    public ConsoleEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendWelcomeEmail(RegisterRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getEmail());
        message.setSubject("Bienvenido a TuHospedaje");
        message.setText("Hola " + request.getFirstName() + ",\n\n"
                + "Gracias por registrarte en TuHospedaje.\n"
                + "Tu cuenta fue creada exitosamente.\n\n"
                + "Email: " + request.getEmail() + "\n\n"
                + "Ya podés iniciar sesión en: http://localhost:5173/login\n\n"
                + "¡Esperamos que disfrutes la experiencia!\n"
                + "Equipo TuHospedaje");

        try {
            mailSender.send(message);
            log.info("Email de bienvenida enviado a {}", request.getEmail());
        } catch (Exception e) {
            log.error("Error al enviar email a {}: {}", request.getEmail(), e.getMessage());
        }
    }
}
