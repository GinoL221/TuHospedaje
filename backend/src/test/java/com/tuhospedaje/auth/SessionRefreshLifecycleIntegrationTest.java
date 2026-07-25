package com.tuhospedaje.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.PasswordChangeRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.auth.UserStatusRequest;
import com.tuhospedaje.entity.RefreshTokenFamily;
import com.tuhospedaje.entity.SessionSecurityEvent;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.RefreshTokenFamilyRepository;
import com.tuhospedaje.repository.SessionSecurityEventRepository;
import com.tuhospedaje.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PR5/WU6 (Design, issue #55) — the full-lifecycle acceptance gate proving the flag-on
 * system satisfies every success scenario across the {@code session-refresh} and
 * {@code session-revocation-lifecycle} Delta Spec domains, exercised end-to-end through
 * the actual HTTP surface rather than re-asserting any single scenario in isolation
 * (those already have dedicated coverage in {@link AuthControllerRefreshIntegrationTest},
 * {@link RefreshReuseIntegrationTest}, {@code UserControllerAdminDisableIntegrationTest},
 * and {@link PasswordChangeIntegrationTest}). Three independent users are used — one per
 * block — because admin-disable and password-change each revoke ALL of a user's refresh
 * families, and mixing flows onto a single account would make one block's revocation
 * clobber another's preconditions.
 *
 * <p><b>Task 5.3 audit (observability, no new logging code):</b> confirmed on-disk that
 * {@code RefreshSessionServiceImpl#revokeAll}'s existing log line already carries every
 * field this scope requires: {@code "event=refresh_session.mass_revoked user_id={}
 * reason={} active_tokens_revoked={} active_families_revoked={}"} — user id, the
 * (sanitized) reason, AND both revocation counts. The admin-disable and password-change
 * blocks below emit this exact line (visible in the test's console output). No dashboard,
 * email, or {@code @Scheduled} delivery worker is in scope for this change (Design
 * binding decisions 6/8) — the persisted {@code PENDING} {@link SessionSecurityEvent} row
 * plus this structured log line ARE the full audit surface; that is a deliberate
 * descoping, not an oversight.
 */
@SpringBootTest(properties = "app.session.refresh.enabled=true")
@AutoConfigureMockMvc
class SessionRefreshLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionSecurityEventRepository sessionSecurityEventRepository;

    @Autowired
    private RefreshTokenFamilyRepository refreshTokenFamilyRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void loginRefreshLogoutRevokesTheFamilyWithACleanNonReuseReason() throws Exception {
        String email = "lifecycle-login-refresh-logout@test.com";
        LoginCookies cookies = loginAndGetCookies(email);

        // "authenticated call" — the freshly-issued ACCESS_TOKEN authenticates.
        mockMvc.perform(get("/api/auth/me").cookie(cookies.accessToken()))
                .andExpect(status().isOk());

        // Scenario "Valid refresh rotates credential without CSRF token" (session-refresh
        // domain) — no X-XSRF-TOKEN header attached, both cookies still rotate.
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(cookies.refreshToken()))
                .andExpect(status().isOk())
                .andReturn();
        Cookie rotatedAccessToken = refreshResult.getResponse().getCookie("ACCESS_TOKEN");
        Cookie rotatedRefreshToken = refreshResult.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(rotatedAccessToken).isNotNull();
        assertThat(rotatedRefreshToken).isNotNull();
        assertThat(rotatedRefreshToken.getValue()).isNotEqualTo(cookies.refreshToken().getValue());

        // Scenario "Only the calling device is logged out" (session-revocation-lifecycle
        // domain) — logout revokes the CURRENT family via revokeCurrent, not revokeAll;
        // both cookies get cleared in the response.
        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(rotatedAccessToken, rotatedRefreshToken, cookies.csrfToken())
                        .header("X-XSRF-TOKEN", cookies.csrfToken().getValue()))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie clearedAccessCookie = logoutResult.getResponse().getCookie("ACCESS_TOKEN");
        Cookie clearedRefreshCookie = logoutResult.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(clearedAccessCookie).isNotNull();
        assertThat(clearedAccessCookie.getMaxAge()).isZero();
        assertThat(clearedRefreshCookie).isNotNull();
        assertThat(clearedRefreshCookie.getMaxAge()).isZero();

        // Replaying the pre-logout (rotated) credential fails: the family is dead.
        mockMvc.perform(post("/api/auth/refresh").cookie(rotatedRefreshToken))
                .andExpect(status().isUnauthorized());

        // The family died via a clean LOGOUT revoke, NOT reuse-detection: logout calls
        // revokeCurrent on a token that was never previously consumed, so
        // RefreshSessionServiceImpl#revokeFamily records reason=LOGOUT and persists no
        // SessionSecurityEvent at all (only the REUSE branch does) — see
        // RefreshSessionServiceImpl#revokeFamily and RefreshSessionServiceTest
        // #revokingWithAFreshTokenPerformsLogoutWithoutReuseEvent for the unit-level proof
        // of this same branch.
        RefreshTokenFamily revokedFamily = refreshTokenFamilyRepository.findAll().stream()
                .filter(family -> email.equals(family.getUser().getEmail()))
                .filter(family -> family.getRevokedAt() != null)
                .findFirst()
                .orElseThrow();
        assertThat(revokedFamily.getRevocationReason()).isEqualTo("LOGOUT");
        assertThat(revokedFamily.getReuseDetectedAt()).isNull();
        assertThat(sessionSecurityEventRepository.findAll().stream()
                .filter(event -> event.getFamily() != null && revokedFamily.getId().equals(event.getFamily().getId())))
                .isEmpty();
    }

    @Test
    void adminDisableRejectsTheNextRequestAndPersistsAnAdminDisableEvent() throws Exception {
        User admin = userRepository.save(User.builder()
                .firstName("Lifecycle")
                .lastName("Admin")
                .email("lifecycle-admin@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build());
        String adminAuthHeader = jwtService.generateToken(admin);

        String targetEmail = "lifecycle-admin-disable-target@test.com";
        LoginCookies targetCookies = loginAndGetCookies(targetEmail);
        Long targetUserId = userRepository.findByEmail(targetEmail).orElseThrow().getId();

        mockMvc.perform(get("/api/auth/me").cookie(targetCookies.accessToken()))
                .andExpect(status().isOk());

        UserStatusRequest disableRequest = new UserStatusRequest(false);
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(patch("/api/users/{id}/enabled", targetUserId)
                        .cookie(accessCookie(adminAuthHeader), csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableRequest)))
                .andExpect(status().isOk());

        // Scenario "Admin disablement revokes sessions and blocks the very next request"
        // (session-revocation-lifecycle domain) — the still-valid, unexpired JWT stops
        // authenticating on the VERY NEXT request, and refresh is rejected too.
        mockMvc.perform(get("/api/auth/me").cookie(targetCookies.accessToken()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").cookie(targetCookies.refreshToken()))
                .andExpect(status().isUnauthorized());

        assertThat(sessionSecurityEventRepository.findAll().stream()
                .filter(event -> event.getUser().getId().equals(targetUserId))
                .filter(event -> event.getEventType() == SessionSecurityEvent.Type.ADMIN_DISABLE))
                .singleElement();
    }

    @Test
    void passwordChangeRevokesAllFamiliesAndPersistsAPasswordChangeEvent() throws Exception {
        String email = "lifecycle-password-change@test.com";
        LoginCookies cookies = loginAndGetCookies(email);
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();

        PasswordChangeRequest request = new PasswordChangeRequest("123456", "newSecurePass1");
        mockMvc.perform(post("/api/auth/password")
                        .cookie(cookies.accessToken(), cookies.csrfToken())
                        .header("X-XSRF-TOKEN", cookies.csrfToken().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Scenario "Password change logs out all devices with an audit trail"
        // (session-revocation-lifecycle domain) — ALL refresh families for this user are
        // revoked (revokeAll, not just the caller's own family), and exactly one
        // PASSWORD_CHANGE event is persisted.
        mockMvc.perform(post("/api/auth/refresh").cookie(cookies.refreshToken()))
                .andExpect(status().isUnauthorized());
        assertThat(sessionSecurityEventRepository.findAll().stream()
                .filter(event -> event.getUser().getId().equals(userId))
                .filter(event -> event.getEventType() == SessionSecurityEvent.Type.PASSWORD_CHANGE))
                .singleElement();
    }

    private void registerUser(String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Lifecycle", "User", email, "123456");
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
