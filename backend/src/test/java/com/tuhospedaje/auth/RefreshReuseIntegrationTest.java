package com.tuhospedaje.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.entity.SessionSecurityEvent;
import com.tuhospedaje.repository.RefreshTokenFamilyRepository;
import com.tuhospedaje.repository.SessionSecurityEventRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers Scenario "Invalid/missing/reused refresh token rejected generically" (Delta
 * Spec, session-refresh domain) specifically for the REUSE case (Design PR2/WU3). The
 * service core ({@link com.tuhospedaje.service.impl.RefreshSessionServiceImpl#rotate})
 * already detects reuse and revokes the whole family, so this suite is HTTP-level
 * hardening — proving the response shape is non-disclosing and the family-wide
 * revocation plus its single audit event are observable from the endpoint, not adding
 * new revocation logic.
 *
 * <p>A single generation replay would be tolerated as a safe retry within
 * {@code app.session.refresh.retry-grace} (default {@code PT5S}) — see
 * {@code RefreshSessionServiceImpl#isEligibleRetry}. To trigger genuine reuse detection
 * (not the retry-grace path), this test rotates TWICE before replaying the original
 * credential, mirroring {@code AuthServiceRefreshIntegrationTest
 * #reuseDetectedThroughRefreshEndpointPersistsFamilyRevocationDespiteTheRejection} but
 * at the HTTP/MockMvc level.
 */
@SpringBootTest(properties = "app.session.refresh.enabled=true")
@AutoConfigureMockMvc
class RefreshReuseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionSecurityEventRepository sessionSecurityEventRepository;

    @Autowired
    private RefreshTokenFamilyRepository refreshTokenFamilyRepository;

    private static final Pattern JWT_SHAPE = Pattern.compile("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    @Test
    void replayingATwiceSupersededCredentialRevokesTheWholeFamilyWithOneAuditEvent() throws Exception {
        String email = "reuse-detect@test.com";
        Cookie originalCredential = loginAndGetRefreshCookie(email);

        Cookie firstRotation = rotate(originalCredential);
        Cookie secondRotation = rotate(firstRotation);

        // Replaying the twice-superseded original credential is genuine reuse: the
        // family is already at generation 2, so the retry-grace window (which only
        // tolerates a same-generation double-submit) does not apply here.
        MvcResult invalidResult = mockMvc.perform(post("/api/auth/refresh").cookie(originalCredential))
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult missingResult = mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Same generic, non-disclosing shape as any other invalid refresh attempt
        // (Delta Spec: reused-credential 401 must be indistinguishable from a
        // missing-cookie 401 — reuses the exact shape assertion already established by
        // AuthControllerRefreshIntegrationTest, not a new contract).
        assertThat(invalidResult.getResponse().getContentAsString())
                .isEqualTo(missingResult.getResponse().getContentAsString());
        assertGenericNonDisclosingBody(invalidResult.getResponse().getContentAsString());
        assertThat(invalidResult.getResponse().getCookie("ACCESS_TOKEN")).isNull();

        // The whole family died, not just the replayed generation: the previously-valid
        // newest credential must also be rejected now.
        mockMvc.perform(post("/api/auth/refresh").cookie(secondRotation))
                .andExpect(status().isUnauthorized());

        // NOTE: both register() and the subsequent login() issue their own refresh
        // family (universal issuance, Design PR1/WU2), so this user ends up with two
        // families — only the one actually rotated/replayed above (from login) gets
        // revoked. Filter on revokedAt, not just email, to target the right one.
        Long familyId = refreshTokenFamilyRepository.findAll().stream()
                .filter(family -> email.equals(family.getUser().getEmail()))
                .filter(family -> family.getRevokedAt() != null)
                .findFirst()
                .orElseThrow()
                .getId();
        long reuseEventCount = sessionSecurityEventRepository.findAll().stream()
                .filter(event -> event.getFamily() != null && familyId.equals(event.getFamily().getId()))
                .filter(event -> event.getEventType() == SessionSecurityEvent.Type.REFRESH_REUSE)
                .filter(event -> event.getDeliveryState() == SessionSecurityEvent.DeliveryState.PENDING)
                .count();
        assertThat(reuseEventCount).isEqualTo(1);
    }

    private Cookie rotate(Cookie refreshCookie) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andReturn();
        Cookie rotated = result.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(rotated).isNotNull();
        return rotated;
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
        RegisterRequest request = new RegisterRequest("Reuse", "Detect", email, "123456");
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
