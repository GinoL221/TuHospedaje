package com.tuhospedaje.configuration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the {@code ACCESS_TOKEN} httpOnly cookie used to carry the signed JWT
 * (Design Decision 1). Reused by login, register, and logout.
 */
@Component
public class AuthCookieFactory {

    private static final String COOKIE_NAME = "ACCESS_TOKEN";
    private static final String COOKIE_PATH = "/";

    private final CookieProperties cookieProperties;
    private final JwtService jwtService;

    public AuthCookieFactory(CookieProperties cookieProperties, JwtService jwtService) {
        this.cookieProperties = cookieProperties;
        this.jwtService = jwtService;
    }

    public ResponseCookie buildAccessTokenCookie(String jwt) {
        return ResponseCookie.from(COOKIE_NAME, jwt)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(jwtService.getExpirationSeconds()))
                .build();
    }

    public ResponseCookie buildClearingCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
