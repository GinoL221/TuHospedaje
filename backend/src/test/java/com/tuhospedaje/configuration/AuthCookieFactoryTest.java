package com.tuhospedaje.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the auth cookie builder (Design Decision 1, Spec Scenario 1.1/2.1/6.1).
 */
class AuthCookieFactoryTest {

    private JwtService jwtService;
    private AuthCookieFactory authCookieFactory;

    @BeforeEach
    void setUp() {
        jwtService = Mockito.mock(JwtService.class);
        Mockito.when(jwtService.getExpirationSeconds()).thenReturn(28800L);
        CookieProperties cookieProperties = new CookieProperties(true, "Strict");
        authCookieFactory = new AuthCookieFactory(cookieProperties, jwtService);
    }

    @Test
    void buildAccessTokenCookieHasExactAttributes() {
        ResponseCookie cookie = authCookieFactory.buildAccessTokenCookie("the-jwt-value");

        assertThat(cookie.getName()).isEqualTo("ACCESS_TOKEN");
        assertThat(cookie.getValue()).isEqualTo("the-jwt-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(28800L);
    }

    @Test
    void buildClearingCookieExpiresImmediately() {
        ResponseCookie cookie = authCookieFactory.buildClearingCookie();

        assertThat(cookie.getName()).isEqualTo("ACCESS_TOKEN");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(0L);
    }
}
