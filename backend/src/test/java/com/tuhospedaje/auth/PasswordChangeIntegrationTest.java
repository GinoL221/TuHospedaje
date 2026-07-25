package com.tuhospedaje.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.PasswordChangeRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.entity.SessionSecurityEvent;
import com.tuhospedaje.repository.SessionSecurityEventRepository;
import com.tuhospedaje.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers Delta Spec scenario "Password change logs out all devices with an audit trail"
 * (session-revocation-lifecycle domain) with refresh sessions genuinely enabled — a
 * separate Spring context from {@link AuthControllerIntegrationTest}, which deliberately
 * runs with the flag off. Mirrors {@link
 * com.tuhospedaje.user.UserControllerAdminDisableIntegrationTest}'s revocation-and-event
 * assertion shape for the password-change trigger instead of admin-disable.
 */
@SpringBootTest(properties = "app.session.refresh.enabled=true")
@AutoConfigureMockMvc
class PasswordChangeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionSecurityEventRepository sessionSecurityEventRepository;

    @Test
    void changingPasswordRevokesAllSessionsClearsCallerCookiesAndPersistsEvent() throws Exception {
        String email = "password-change-ok@test.com";
        LoginCookies cookies = loginAndGetCookies(email);
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();

        PasswordChangeRequest request = new PasswordChangeRequest("123456", "newSecurePass1");
        MvcResult result = mockMvc.perform(post("/api/auth/password")
                        .cookie(cookies.accessToken(), cookies.csrfToken())
                        .header("X-XSRF-TOKEN", cookies.csrfToken().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("ACCESS_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("REFRESH_TOKEN="))))
                .andReturn();

        Cookie clearedAccessCookie = result.getResponse().getCookie("ACCESS_TOKEN");
        Cookie clearedRefreshCookie = result.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(clearedAccessCookie).isNotNull();
        assertThat(clearedAccessCookie.getMaxAge()).isZero();
        assertThat(clearedRefreshCookie).isNotNull();
        assertThat(clearedRefreshCookie.getMaxAge()).isZero();

        // Every refresh family for this user is revoked — the previously-valid
        // REFRESH_TOKEN can no longer rotate.
        mockMvc.perform(post("/api/auth/refresh").cookie(cookies.refreshToken()))
                .andExpect(status().isUnauthorized());

        assertThat(sessionSecurityEventRepository.findAll().stream()
                .filter(event -> event.getUser().getId().equals(userId))
                .filter(event -> event.getEventType() == SessionSecurityEvent.Type.PASSWORD_CHANGE))
                .singleElement();

        // The new password authenticates; the old one no longer does.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "newSecurePass1"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "123456"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongCurrentPasswordIsRejectedWithoutRevokingOrPersistingAnEvent() throws Exception {
        String email = "password-change-wrong-current@test.com";
        LoginCookies cookies = loginAndGetCookies(email);
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();

        PasswordChangeRequest request = new PasswordChangeRequest("not-the-real-password", "newSecurePass1");
        mockMvc.perform(post("/api/auth/password")
                        .cookie(cookies.accessToken(), cookies.csrfToken())
                        .header("X-XSRF-TOKEN", cookies.csrfToken().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(sessionSecurityEventRepository.findAll().stream()
                .filter(event -> event.getUser().getId().equals(userId))
                .filter(event -> event.getEventType() == SessionSecurityEvent.Type.PASSWORD_CHANGE))
                .isEmpty();

        // The family was never revoked — the original REFRESH_TOKEN still rotates fine.
        mockMvc.perform(post("/api/auth/refresh").cookie(cookies.refreshToken()))
                .andExpect(status().isOk());
    }

    @Test
    void weakNewPasswordIsRejectedAsAValidationError() throws Exception {
        LoginCookies cookies = loginAndGetCookies("password-change-weak-new@test.com");

        PasswordChangeRequest request = new PasswordChangeRequest("123456", "abc");
        mockMvc.perform(post("/api/auth/password")
                        .cookie(cookies.accessToken(), cookies.csrfToken())
                        .header("X-XSRF-TOKEN", cookies.csrfToken().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private void registerUser(String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", email, "123456");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private LoginCookies loginAndGetCookies(String email) throws Exception {
        registerUser(email);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "123456"))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie accessTokenCookie = result.getResponse().getCookie("ACCESS_TOKEN");
        Cookie refreshTokenCookie = result.getResponse().getCookie("REFRESH_TOKEN");
        Cookie csrfTokenCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(accessTokenCookie).isNotNull();
        assertThat(refreshTokenCookie).isNotNull();
        assertThat(csrfTokenCookie).isNotNull();
        return new LoginCookies(accessTokenCookie, refreshTokenCookie, csrfTokenCookie);
    }

    private record LoginCookies(Cookie accessToken, Cookie refreshToken, Cookie csrfToken) {
    }
}
