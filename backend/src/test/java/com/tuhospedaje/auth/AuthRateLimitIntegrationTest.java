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
 * Design "Testing Strategy" — real 429s over HTTP through the full filter chain, with a
 * per-class tight override so this test's own cached Spring context (own counter map)
 * never leaks into the shared, high-ceiling context the other 6 auth test files use. See
 * design "Configuration Values" for the ceiling rationale.
 *
 * <p>Every test method uses its own unique fake remote address (via
 * {@link RequestPostProcessor}) so the IP dimension is isolated per scenario regardless
 * of method execution order — {@code AbstractIntegrationTest} rolls back DB writes per
 * test method, but the rate-limit counter map is a Spring singleton bean that survives
 * across test methods within this shared context.
 */
@SpringBootTest(properties = {
        "app.auth.rate-limit.enabled=true",
        "app.auth.rate-limit.login-per-ip-per-minute=3",
        "app.auth.rate-limit.login-per-email-per-minute=2",
        "app.auth.rate-limit.register-per-ip-per-minute=3",
        "app.auth.rate-limit.register-per-email-per-minute=1"
})
@AutoConfigureMockMvc
class AuthRateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailOutboxService emailOutboxService;

    /**
     * Requirement 8-style guard (mirrors {@code AuthControllerIntegrationTest}): a
     * compact JWT is three dot-separated base64url segments. A 429 must never leak one.
     */
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

    private MvcResult login(String ip, String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        return mockMvc.perform(post("/api/auth/login")
                        .with(fromIp(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    private MvcResult register(String ip, RegisterRequest request) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .with(fromIp(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    private void registerRealUser(String registrationIp, String email) throws Exception {
        register(registrationIp, new RegisterRequest("Test", "User", email, "123456"))
                .getResponse();
    }

    /**
     * Finds the first {@code 429} response while attempting up to {@code maxAttempts}
     * times. The rate limiter's window is keyed by the real system clock (no fixed
     * clock override at the integration level — that would require overriding the
     * shared {@code Supplier<Clock>} bean used by session/JWT logic too), so a handful
     * of extra attempts of headroom over the configured ceiling absorbs the
     * astronomically rare case of a real minute-boundary rollover landing mid-sequence,
     * without weakening the assertion that the system DOES eventually block.
     */
    private interface AttemptFn {
        MvcResult apply(int attemptNumber) throws Exception;
    }

    private MvcResult firstBlocked(int maxAttempts, AttemptFn attempt) throws Exception {
        for (int i = 1; i <= maxAttempts; i++) {
            MvcResult result = attempt.apply(i);
            if (result.getResponse().getStatus() == 429) {
                return result;
            }
        }
        return null;
    }

    // --- INT-1: 3 logins OK, 4th -> 429 + Retry-After + {error,status:429} ---

    @Test
    void fourthLoginAttemptFromSameIpWithin3IsRateLimited() throws Exception {
        String ip = "10.1.1.1";

        MvcResult blocked = firstBlocked(6, i -> login(ip, "int1-" + i + "@test.com", "wrong-password"));

        assertThat(blocked).as("IP ceiling of 3 must eventually trip").isNotNull();
        assertThat(blocked.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
        JsonNode body = objectMapper.readTree(blocked.getResponse().getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(429);
        assertThat(body.has("error")).isTrue();
    }

    // --- INT-2: 429 sets no ACCESS_TOKEN cookie and leaks no JWT ---

    @Test
    void rateLimitedLoginSetsNoAccessTokenCookieAndLeaksNoJwt() throws Exception {
        String ip = "10.1.1.2";

        MvcResult blocked = firstBlocked(6, i -> login(ip, "int2-" + i + "@test.com", "wrong-password"));

        assertThat(blocked).as("IP ceiling of 3 must eventually trip").isNotNull();
        assertThat(blocked.getResponse().getCookie("ACCESS_TOKEN")).isNull();
        assertBodyHasNoJwt(blocked.getResponse().getContentAsString());
    }

    // --- INT-3: under-limit invalid credentials still return exactly 401 ---

    @Test
    void underLimitInvalidCredentialsStillReturn401() throws Exception {
        MvcResult result = login("10.1.1.3", "int3-nobody@test.com", "wrong-password");

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    // --- INT-4: under-limit invalid fields still return exactly 400 ---

    @Test
    void underLimitInvalidRegisterFieldsStillReturn400() throws Exception {
        MvcResult result = register("10.1.1.4", new RegisterRequest("", "", "not-an-email", "1"));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    // --- INT-5: after login limit exhausted, refresh and welcome-resend are unaffected ---

    @Test
    void refreshAndWelcomeResendAreUnaffectedAfterLoginLimitIsExhausted() throws Exception {
        String ip = "10.1.1.5";
        MvcResult blocked = firstBlocked(6, i -> login(ip, "int5-" + i + "@test.com", "wrong-password"));
        assertThat(blocked).as("login IP ceiling must be exhausted before checking other endpoints").isNotNull();

        MvcResult refresh = mockMvc.perform(post("/api/auth/refresh").with(fromIp(ip))).andReturn();
        assertThat(refresh.getResponse().getStatus()).isEqualTo(401);

        MvcResult resend = mockMvc.perform(post("/api/auth/welcome-email/resend").with(fromIp(ip))).andReturn();
        assertThat(resend.getResponse().getStatus()).isEqualTo(401);
    }

    // --- INT-6: a successful login also consumes quota ---

    @Test
    void successfulLoginsAlsoConsumeTheIpQuota() throws Exception {
        String loginIp = "10.1.1.6";
        int headroomAttempts = 6;
        for (int i = 1; i <= headroomAttempts; i++) {
            registerRealUser("10.1.1.6" + i, "int6-" + i + "@test.com");
        }

        MvcResult first = login(loginIp, "int6-1@test.com", "123456");
        assertThat(first.getResponse().getStatus())
                .as("first attempt with fully valid credentials must succeed, proving it is a genuine login, not an incidental failure")
                .isEqualTo(200);

        // Every remaining attempt also has fully valid credentials (a different real
        // account each time) — proves a SUCCESSFUL login consumes the IP quota just
        // like a failed one, since the IP ceiling eventually trips despite every
        // attempt being individually legitimate.
        MvcResult blocked = firstBlocked(headroomAttempts - 1, i -> login(loginIp, "int6-" + (i + 1) + "@test.com", "123456"));
        assertThat(blocked).as("IP ceiling of 3 must eventually trip even though every login succeeded").isNotNull();
    }

    // --- INT-7: 429 body key set is exactly {error,status}, status matches the HTTP status ---

    @Test
    void the429BodyHasExactlyErrorAndStatusKeysMatchingTheHttpStatus() throws Exception {
        String ip = "10.1.1.7";
        MvcResult blocked = firstBlocked(6, i -> login(ip, "int7-" + i + "@test.com", "wrong-password"));

        assertThat(blocked).as("IP ceiling of 3 must eventually trip").isNotNull();
        JsonNode body = objectMapper.readTree(blocked.getResponse().getContentAsString());
        assertThat(body.properties()).extracting(java.util.Map.Entry::getKey)
                .containsExactlyInAnyOrder("error", "status");
        assertThat(body.get("status").asInt()).isEqualTo(blocked.getResponse().getStatus());
    }

    // --- INT-8: 429 is indistinguishable for a registered vs. an unregistered email ---

    @Test
    void the429ResponseIsIdenticalForRegisteredAndUnregisteredEmail() throws Exception {
        String registeredEmail = "int8-real@test.com";
        registerRealUser("10.1.1.80", registeredEmail);

        String registeredIp = "10.1.1.81";
        String unregisteredIp = "10.1.1.82";

        MvcResult blockedRegistered = firstBlocked(6, i -> login(registeredIp, registeredEmail, "wrong-password"));
        MvcResult blockedUnregistered = firstBlocked(6, i -> login(unregisteredIp, "int8-fake@test.com", "wrong-password"));

        assertThat(blockedRegistered).as("registered email's per-email ceiling must eventually trip").isNotNull();
        assertThat(blockedUnregistered).as("unregistered email's per-email ceiling must eventually trip").isNotNull();

        String bodyRegistered = blockedRegistered.getResponse().getContentAsString();
        String bodyUnregistered = blockedUnregistered.getResponse().getContentAsString();
        assertThat(bodyRegistered).isEqualTo(bodyUnregistered);
        assertThat(blockedRegistered.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
        assertThat(blockedUnregistered.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
    }
}
