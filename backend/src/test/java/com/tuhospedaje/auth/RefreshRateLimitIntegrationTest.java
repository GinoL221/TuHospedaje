package com.tuhospedaje.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.service.EmailOutboxService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Design "Testing Strategy" — real 429s over HTTP through the full filter+service+handler
 * chain for BOTH refresh-rate-limit dimensions, with a per-class tight override so this
 * test's own cached Spring context (own counter maps) never leaks into the shared,
 * high-ceiling context {@code AuthControllerIntegrationTest} uses. Mirrors the {@code
 * AuthRateLimitIntegrationTest} idiom (PR1).
 *
 * <p>{@code refresh-per-ip-per-minute=5} is deliberately generous relative to {@code
 * refresh-per-family-per-minute=3}: I-2/I-3/I-6/I-7 need at least
 * {@code FAMILY_LIMIT + 1 = 4} refresh calls against the SAME IP to trip the family
 * ceiling, which must happen strictly before the IP ceiling would ever trip on that same
 * IP. I-1 uses its own dedicated IP and enough attempts (6) to trip the IP ceiling
 * instead, with no family ever resolved (garbage credential).
 */
@SpringBootTest(properties = {
        "app.session.refresh.enabled=true",
        "app.session.rate-limit.enabled=true",
        "app.session.rate-limit.refresh-per-ip-per-minute=5",
        "app.session.rate-limit.refresh-per-family-per-minute=3"
})
@AutoConfigureMockMvc
class RefreshRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final int FAMILY_LIMIT = 3;
    private static final int IP_LIMIT = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailOutboxService emailOutboxService;

    /** Requirement 8-style guard, same regex as {@code AuthControllerIntegrationTest}. */
    private static final Pattern JWT_SHAPE = Pattern.compile("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    private void assertBodyHasNoJwt(String body) {
        assertThat(JWT_SHAPE.matcher(body).find()).isFalse();
        assertThat(body).doesNotContain("\"token\"");
    }

    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private void registerRealUser(String ip, String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", email, "123456");
        mockMvc.perform(post("/api/auth/register")
                        .with(fromIp(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    /** Index 0 = ACCESS_TOKEN, index 1 = REFRESH_TOKEN. Both are asserted non-null. */
    private Cookie[] loginAndGetCookies(String ip, String email) throws Exception {
        registerRealUser(ip, email);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(fromIp(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "123456"))))
                .andReturn();
        Cookie accessToken = result.getResponse().getCookie("ACCESS_TOKEN");
        Cookie refreshToken = result.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(accessToken).as("login must set ACCESS_TOKEN").isNotNull();
        assertThat(refreshToken).as("login must set REFRESH_TOKEN").isNotNull();
        return new Cookie[]{accessToken, refreshToken};
    }

    private MvcResult refresh(String ip, Cookie refreshTokenCookie) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                        .with(fromIp(ip))
                        .cookie(refreshTokenCookie))
                .andReturn();
    }

    private MvcResult refreshWithGarbageCredential(String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                        .with(fromIp(ip))
                        .cookie(new Cookie("REFRESH_TOKEN", "not-a-real-refresh-token")))
                .andReturn();
    }

    private MvcResult triggerIpRateLimit(String ip) throws Exception {
        for (int i = 1; i <= IP_LIMIT + 1; i++) {
            MvcResult result = refreshWithGarbageCredential(ip);
            if (result.getResponse().getStatus() == 429) {
                return result;
            }
        }
        throw new AssertionError("IP ceiling of " + IP_LIMIT + " never tripped for " + ip);
    }

    private MvcResult triggerFamilyRateLimit(String ip, String email) throws Exception {
        Cookie refreshTokenCookie = loginAndGetCookies(ip, email)[1];
        for (int i = 1; i <= FAMILY_LIMIT + 1; i++) {
            MvcResult result = refresh(ip, refreshTokenCookie);
            if (result.getResponse().getStatus() == 429) {
                return result;
            }
            Cookie rotated = result.getResponse().getCookie("REFRESH_TOKEN");
            assertThat(rotated).as("a successful refresh must rotate REFRESH_TOKEN").isNotNull();
            refreshTokenCookie = rotated;
        }
        throw new AssertionError("Family ceiling of " + FAMILY_LIMIT + " never tripped for " + email);
    }

    // --- I-1: over-limit refresh returns 429 for the IP dimension ---

    @Test
    void ipCeilingExceededReturns429WithRetryAfter() throws Exception {
        MvcResult blocked = triggerIpRateLimit("10.2.1.1");

        assertThat(blocked.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
        JsonNode body = objectMapper.readTree(blocked.getResponse().getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(429);
        assertThat(body.has("error")).isTrue();
    }

    // --- I-2: over-limit refresh returns 429 for the family dimension ---

    @Test
    void familyCeilingExceededReturns429WithRetryAfter() throws Exception {
        MvcResult blocked = triggerFamilyRateLimit("10.2.1.2", "i2-family@test.com");

        assertThat(blocked.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
        JsonNode body = objectMapper.readTree(blocked.getResponse().getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(429);
        assertThat(body.has("error")).isTrue();
    }

    // --- I-3: the family 429 sets no ACCESS_TOKEN, does not clear REFRESH_TOKEN, leaks
    // no JWT, and carries no family id ---

    @Test
    void familyRateLimited429LeaksNoCookiesJwtOrFamilyId() throws Exception {
        MvcResult blocked = triggerFamilyRateLimit("10.2.1.3", "i3-family@test.com");

        assertThat(blocked.getResponse().getCookie("ACCESS_TOKEN")).isNull();
        Cookie refreshCookieInResponse = blocked.getResponse().getCookie("REFRESH_TOKEN");
        if (refreshCookieInResponse != null) {
            assertThat(refreshCookieInResponse.getMaxAge())
                    .as("a family 429 must never CLEAR the still-valid REFRESH_TOKEN")
                    .isNotZero();
        }
        String body = blocked.getResponse().getContentAsString();
        assertBodyHasNoJwt(body);
        assertThat(body).doesNotContain("familyId", "family_id");
    }

    // --- I-4: under the ceiling, an invalid credential still returns exactly 401 ---

    @Test
    void underCeilingInvalidCredentialStillReturns401() throws Exception {
        MvcResult result = refreshWithGarbageCredential("10.2.1.4");

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    // --- I-5: under the ceiling, a valid refresh still returns 200 with both cookies
    // rotated ---

    @Test
    void underCeilingValidRefreshStillReturns200WithRotatedCookies() throws Exception {
        Cookie refreshTokenCookie = loginAndGetCookies("10.2.1.5", "i5-valid@test.com")[1];

        MvcResult result = refresh("10.2.1.5", refreshTokenCookie);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getCookie("ACCESS_TOKEN")).isNotNull();
        Cookie rotatedRefreshToken = result.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(rotatedRefreshToken).isNotNull();
        assertThat(rotatedRefreshToken.getValue()).isNotEqualTo(refreshTokenCookie.getValue());
    }

    // --- I-6: the filter-written (IP) and handler-written (family) 429 bodies are
    // byte-equivalent — the divergence guard ---

    @Test
    void ipAndFamily429BodiesAreByteEquivalent() throws Exception {
        MvcResult ipBlocked = triggerIpRateLimit("10.2.1.6");
        MvcResult familyBlocked = triggerFamilyRateLimit("10.2.1.66", "i6-family@test.com");

        assertThat(ipBlocked.getResponse().getContentAsString())
                .isEqualTo(familyBlocked.getResponse().getContentAsString());
    }

    // --- I-7: logout still succeeds after the family ceiling is exhausted ---

    @Test
    void logoutStillSucceedsAfterFamilyCeilingExhausted() throws Exception {
        String ip = "10.2.1.7";
        triggerFamilyRateLimit(ip, "i7-logout@test.com");

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/auth/logout")
                        .with(fromIp(ip))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }
}
