package com.tuhospedaje.service;
import com.tuhospedaje.configuration.WelcomeEmailProperties;
import com.tuhospedaje.dto.email.EmailMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
@Service
public class WelcomeEmailRenderer {

    private static final String WELCOME = "WELCOME";
    private static final String SUBJECT = "¡Bienvenido a TuHospedaje!";

    private final WelcomeEmailProperties properties;

    public WelcomeEmailRenderer(WelcomeEmailProperties properties) {
        this.properties = properties;
    }

    public EmailMessage render(Long userId, String recipient, String firstName) {
        String loginUrl = properties.getNormalizedPublicBaseUrl() + "/login";
        String escapedName = HtmlUtils.htmlEscape(firstName);
        String escapedLoginUrl = HtmlUtils.htmlEscape(loginUrl);
        String body = """
                <html><body style="font-family:sans-serif;color:#222;">
                <h2 style="color:#c0392b;">¡Bienvenido a TuHospedaje, %s!</h2>
                <p>Gracias por registrarte. Tu cuenta ya está lista.</p>
                <p><a href="%s">Iniciar sesión</a> para descubrir alojamientos.</p>
                <hr><p style="font-size:12px;color:#888;">TuHospedaje</p>
                </body></html>
                """.formatted(escapedName, escapedLoginUrl);
        return new EmailMessage(recipient, SUBJECT, body, WELCOME, userId.toString());
    }
}
