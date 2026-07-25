package com.tuhospedaje.configuration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the {@code REFRESH_TOKEN} httpOnly cookie used to carry the renewable
 * refresh credential (Design PR1/WU2, mirrors {@link AuthCookieFactory}).
 * Scoped to {@code path=/api/auth} (not {@code /}) so the long-lived (30-day)
 * credential is only ever sent to auth endpoints, not every request.
 */
@Component
public class RefreshCookieFactory {

    private static final String COOKIE_NAME = "REFRESH_TOKEN";
    private static final String COOKIE_PATH = "/api/auth";

    private final CookieProperties cookieProperties;
    private final SessionProperties sessionProperties;

    public RefreshCookieFactory(CookieProperties cookieProperties, SessionProperties sessionProperties) {
        this.cookieProperties = cookieProperties;
        this.sessionProperties = sessionProperties;
    }

    public ResponseCookie buildRefreshCookie(String refreshCredential) {
        return ResponseCookie.from(COOKIE_NAME, refreshCredential)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(COOKIE_PATH)
                .maxAge(sessionProperties.refresh().absoluteLifetime())
                .build();
    }

    public ResponseCookie buildClearingRefreshCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
