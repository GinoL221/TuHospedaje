package com.tuhospedaje.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers Scenarios "Valid refresh rotates credential without CSRF token" and "Invalid/
 * missing/reused refresh token rejected generically" (Delta Spec, session-refresh
 * domain) with refresh sessions genuinely enabled — a separate Spring context from
 * {@link AuthControllerIntegrationTest}, which deliberately runs with the flag off.
 */
@SpringBootTest(properties = "app.session.refresh.enabled=true")
@AutoConfigureMockMvc
class AuthControllerRefreshIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Pattern JWT_SHAPE = Pattern.compile("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    @Test
    void loginSetsBothAccessAndRefreshTokenCookies() throws Exception {
        registerUser("refresh-login@test.com");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("refresh-login@test.com", "123456"))))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("ACCESS_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("REFRESH_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("Path=/api/auth"))))
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getValue()).isNotBlank();
    }

    @Test
    void registerSetsBothAccessAndRefreshTokenCookies() throws Exception {
        RegisterRequest request = new RegisterRequest("Refresh", "Register", "refresh-register@test.com", "123456");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("REFRESH_TOKEN="))))
                .andReturn();

        assertThat(result.getResponse().getCookie("REFRESH_TOKEN")).isNotNull();
    }

    @Test
    void refreshWithValidCookieRotatesBothCookiesAndReturns200() throws Exception {
        Cookie refreshCookie = loginAndGetRefreshCookie("refresh-valid@test.com");

        MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("ACCESS_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("REFRESH_TOKEN="))))
                .andReturn();

        Cookie rotatedAccessToken = result.getResponse().getCookie("ACCESS_TOKEN");
        Cookie rotatedRefreshToken = result.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(rotatedAccessToken).isNotNull();
        assertThat(rotatedRefreshToken).isNotNull();
        assertThat(rotatedRefreshToken.getValue()).isNotEqualTo(refreshCookie.getValue());
    }

    @Test
    void refreshWithoutCookieReturns401WithGenericBodyAndNoCookies() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getCookie("ACCESS_TOKEN")).isNull();
        assertGenericNonDisclosingBody(result.getResponse().getContentAsString());
    }

    @Test
    void refreshWithInvalidCookieReturns401WithSameGenericBodyAsMissingCookie() throws Exception {
        MvcResult missingResult = mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult invalidResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("REFRESH_TOKEN", "rt1.unknown.not-a-real-credential")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Non-disclosing (Spec: "Invalid/missing/reused refresh token rejected
        // generically") — a missing cookie and a garbage one must be indistinguishable.
        assertThat(invalidResult.getResponse().getContentAsString())
                .isEqualTo(missingResult.getResponse().getContentAsString());
        assertGenericNonDisclosingBody(invalidResult.getResponse().getContentAsString());
        assertThat(invalidResult.getResponse().getCookie("ACCESS_TOKEN")).isNull();
    }

    @Test
    void refreshDoesNotRequireACsrfHeader() throws Exception {
        Cookie refreshCookie = loginAndGetRefreshCookie("refresh-no-csrf@test.com");

        // No X-XSRF-TOKEN header attached at all — must not be rejected with 403.
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk());
    }

    private Cookie loginAndGetRefreshCookie(String email) throws Exception {
        registerUser(email);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "123456"))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = result.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(refreshCookie).isNotNull();
        return refreshCookie;
    }

    private void registerUser(String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", email, "123456");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private void assertGenericNonDisclosingBody(String body) {
        assertThat(JWT_SHAPE.matcher(body).find())
                .as("Refresh error body must not contain any JWT-shaped string: %s", body)
                .isFalse();
        assertThat(body).contains("\"status\":401");
    }
}
