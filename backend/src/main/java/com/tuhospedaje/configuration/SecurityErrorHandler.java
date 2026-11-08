package com.tuhospedaje.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Writes the 401/403 answers produced by the security filter chain with the same JSON
 * shape as {@code GlobalExceptionHandler}: {@code {"error": "...", "status": N}}.
 *
 * <p>Those answers are produced before any controller runs, so
 * {@code @RestControllerAdvice} never sees them. The status code of each answer is
 * chosen by {@code SecurityConfig}; this class only decides the body.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AccessDeniedHandler {

    static final String AUTH_REQUIRED_KEY = "error.auth.required";
    static final String ACCESS_DENIED_KEY = "error.access_denied";
    static final String CSRF_INVALID_KEY = "error.csrf_invalid";

    private final MessageSource messageSource;
    private final JsonMapper jsonMapper;

    /** Entry point for an unauthenticated request that must answer with {@code status}. */
    public AuthenticationEntryPoint entryPoint(HttpStatus status) {
        String messageKey = status == HttpStatus.UNAUTHORIZED ? AUTH_REQUIRED_KEY : ACCESS_DENIED_KEY;
        return (request, response, authException) -> write(request, response, status, messageKey);
    }

    /**
     * Authenticated caller without the required role, or a request with a missing or invalid
     * CSRF token. The CSRF case has its own message: telling a user whose token expired that
     * they have no permissions would be misleading. The status is 403 in both cases.
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String messageKey = accessDeniedException instanceof CsrfException ? CSRF_INVALID_KEY : ACCESS_DENIED_KEY;
        write(request, response, HttpStatus.FORBIDDEN, messageKey);
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String messageKey) throws IOException {
        String message = messageSource.getMessage(messageKey, null, request.getLocale());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(
                Map.of("error", message, "status", status.value())));
    }
}
