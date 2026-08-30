package com.tuhospedaje.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the refresh cookie builder (Design PR1/WU2). Mirrors
 * {@link AuthCookieFactoryTest}, but the REFRESH_TOKEN cookie differs from
 * ACCESS_TOKEN on path (scoped to /api/auth, not sent on every request) and
 * max-age (the 30-day absolute lifetime, not the 15-minute JWT expiry).
 */
class RefreshCookieFactoryTest {

    private RefreshCookieFactory refreshCookieFactory;

    @BeforeEach
    void setUp() {
        CookieProperties cookieProperties = new CookieProperties(true, "Strict");
        SessionProperties sessionProperties = new SessionProperties(
                Duration.ofMinutes(15),
                new SessionProperties.RefreshProperties(true, Duration.ofDays(30), Duration.ofSeconds(5)),
                new SessionProperties.CleanupProperties(Duration.ofDays(1), 100),
                new SessionProperties.RateLimitProperties(true, 10, 60));
        refreshCookieFactory = new RefreshCookieFactory(cookieProperties, sessionProperties);
    }

    @Test
    void buildRefreshCookieHasExactAttributes() {
        ResponseCookie cookie = refreshCookieFactory.buildRefreshCookie("rt1.key.value");

        assertThat(cookie.getName()).isEqualTo("REFRESH_TOKEN");
        assertThat(cookie.getValue()).isEqualTo("rt1.key.value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void buildClearingRefreshCookieExpiresImmediatelyAndMatchesNameAndPath() {
        ResponseCookie cookie = refreshCookieFactory.buildClearingRefreshCookie();

        assertThat(cookie.getName()).isEqualTo("REFRESH_TOKEN");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }
}
