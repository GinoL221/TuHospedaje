package com.tuhospedaje.configuration;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

/**
 * Design: "Filter Core — Strict TDD". Uses a fixed {@link Clock} so window-boundary
 * tests are deterministic (UNIT-3). Lives beside {@link JwtAuthenticationFilterTest} in
 * the same package for protected-method access ({@code shouldNotFilter}).
 */
@ExtendWith(MockitoExtension.class)
class AuthRateLimitFilterTest {

    private static final AuthRateLimitProperties GENEROUS = new AuthRateLimitProperties(
            true, 1000, 1000, 1000, 1000);

    @Mock
    private MessageSource messageSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void stubRateLimitMessage() {
        lenient().when(messageSource.getMessage(eq("error.rate_limit"), any(), any(Locale.class)))
                .thenReturn("Too many attempts. Try again later.");
    }

    private Supplier<Clock> fixedClock(long epochSecond) {
        Clock clock = Clock.fixed(java.time.Instant.ofEpochSecond(epochSecond), java.time.ZoneOffset.UTC);
        return () -> clock;
    }

    private AuthRateLimitFilter newFilter(AuthRateLimitProperties properties, long epochSecond) {
        return new AuthRateLimitFilter(properties, fixedClock(epochSecond), objectMapper, messageSource);
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContent("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        request.setContentType("application/json");
        return request;
    }

    // --- UNIT-9: kill switch ---

    @Test
    void disabledKillSwitchSkipsFilteringEntirely() throws Exception {
        AuthRateLimitProperties disabled = new AuthRateLimitProperties(false, 1, 1, 1, 1);
        AuthRateLimitFilter filter = newFilter(disabled, 0L);

        assertThat(filter.shouldNotFilter(loginRequest())).isTrue();

        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest request = loginRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    // --- UNIT-10: scoping matrix — everything except POST login/register is skipped ---

    @ParameterizedTest
    @CsvSource({
            "POST,/api/auth/refresh",
            "POST,/api/auth/logout",
            "POST,/api/auth/welcome-email/resend",
            "GET,/api/auth/csrf",
            "GET,/api/auth/me",
            "GET,/api/auth/login"
    })
    void skipsEveryPathOtherThanPostLoginOrRegister(String method, String path) {
        AuthRateLimitFilter filter = newFilter(GENEROUS, 0L);
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    // --- UNIT-11: path-normalization matrix (Threat Matrix — enumeration-safe scoping) ---
    // Failure mode to prevent: a URI that Spring Security's authorization resolves to
    // /api/auth/login (thus permitAll() and routed to the handler) while our filter's
    // matcher does not — an unlimited login path.

    @ParameterizedTest
    @CsvSource({
            "/api/auth/login/",
            "/api/auth//login",
            "/api/auth/Login",
            "/api/AUTH/login",
            "/api/auth/login;jsessionid=x",
            "/api/auth/%6cogin"
    })
    void isRateLimitedForEveryNormalizationVariantOfLoginPath(String path) {
        AuthRateLimitFilter filter = newFilter(GENEROUS, 0L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        assertThat(filter.shouldNotFilter(request))
                .as("path '%s' must not bypass rate limiting via normalization/encoding", path)
                .isFalse();
    }

    // --- UNIT-1/2/3: fixed-window counting ---

    private static final AuthRateLimitProperties TIGHT_LOGIN_IP = new AuthRateLimitProperties(
            true, 2, 1000, 1000, 1000);

    private AuthRateLimitFilter newFilterWithMutableClock(AuthRateLimitProperties properties,
                                                            AtomicReference<Clock> clockHolder) {
        return new AuthRateLimitFilter(properties, clockHolder::get, objectMapper, messageSource);
    }

    @Test
    void nthRequestAtTheLimitStillPassesThrough() throws Exception {
        AtomicReference<Clock> clockHolder = new AtomicReference<>(fixedClockAt(0L));
        AuthRateLimitFilter filter = newFilterWithMutableClock(TIGHT_LOGIN_IP, clockHolder);
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (req, res) -> chainCalls.incrementAndGet();

        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(loginRequest(), response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        assertThat(chainCalls.get()).isEqualTo(2);
    }

    @Test
    void limitPlusOneSameMinuteReturns429AndDoesNotInvokeChain() throws Exception {
        AtomicReference<Clock> clockHolder = new AtomicReference<>(fixedClockAt(0L));
        AuthRateLimitFilter filter = newFilterWithMutableClock(TIGHT_LOGIN_IP, clockHolder);
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (req, res) -> chainCalls.incrementAndGet();

        for (int i = 0; i < 2; i++) {
            filter.doFilter(loginRequest(), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
        assertThat(chainCalls.get()).isEqualTo(2);
    }

    @Test
    void windowResetAfterClockAdvancesPastTheMinuteBoundary() throws Exception {
        AtomicReference<Clock> clockHolder = new AtomicReference<>(fixedClockAt(0L));
        AuthRateLimitFilter filter = newFilterWithMutableClock(TIGHT_LOGIN_IP, clockHolder);
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (req, res) -> chainCalls.incrementAndGet();

        for (int i = 0; i < 2; i++) {
            filter.doFilter(loginRequest(), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), blocked, chain);
        assertThat(blocked.getStatus()).isEqualTo(429);

        clockHolder.set(fixedClockAt(61L));
        MockHttpServletResponse afterReset = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), afterReset, chain);

        assertThat(afterReset.getStatus()).isEqualTo(200);
        assertThat(chainCalls.get()).isEqualTo(3);
    }

    private Clock fixedClockAt(long epochSecond) {
        return Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
    }

    // --- UNIT-12: 429 body shape is exactly {error,status} ---

    private static final AuthRateLimitProperties SINGLE_ATTEMPT = new AuthRateLimitProperties(
            true, 1, 1000, 1000, 1000);

    @Test
    void the429BodyContainsExactlyErrorAndStatusKeys() throws Exception {
        AuthRateLimitFilter filter = newFilter(SINGLE_ATTEMPT, 0L);
        FilterChain chain = (req, res) -> { };

        filter.doFilter(loginRequest(), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        com.fasterxml.jackson.databind.JsonNode body = objectMapper.readTree(blocked.getContentAsByteArray());
        assertThat(body.properties()).extracting(java.util.Map.Entry::getKey)
                .containsExactlyInAnyOrder("error", "status");
        assertThat(body.get("status").asInt()).isEqualTo(429);
        assertThat(body.toString()).doesNotContain("\"token\"");
    }

    // --- UNIT-6/7/8: body re-readability, malformed JSON, oversized body ---

    private MockHttpServletRequest jsonRequest(String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setContent(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        request.setContentType("application/json");
        return request;
    }

    @Test
    void requestBodyIsFullyReReadableByTheChainAfterFiltering() throws Exception {
        AuthRateLimitFilter filter = newFilter(GENEROUS, 0L);
        String originalJson = "{\"email\":\"reader@test.com\",\"password\":\"secret\"}";
        MockHttpServletRequest request = jsonRequest("/api/auth/login", originalJson);
        AtomicReference<jakarta.servlet.http.HttpServletRequest> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set((jakarta.servlet.http.HttpServletRequest) req);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        jakarta.servlet.http.HttpServletRequest downstream = captured.get();
        assertThat(downstream).isNotNull();
        String viaInputStream = new String(downstream.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(viaInputStream).isEqualTo(originalJson);

        String viaReader;
        try (java.io.BufferedReader reader = downstream.getReader()) {
            viaReader = reader.lines().reduce("", (a, b) -> a + b);
        }
        assertThat(viaReader).isEqualTo(originalJson);
    }

    @Test
    void malformedJsonBodyDoesNotThrowAndStillInvokesChain() throws Exception {
        AuthRateLimitFilter filter = newFilter(GENEROUS, 0L);
        MockHttpServletRequest request = jsonRequest("/api/auth/login", "{not-valid-json");
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (req, res) -> chainCalls.incrementAndGet();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(chainCalls.get()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void emptyBodyDoesNotThrowAndStillInvokesChain() throws Exception {
        AuthRateLimitFilter filter = newFilter(GENEROUS, 0L);
        MockHttpServletRequest request = jsonRequest("/api/auth/login", "");
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (req, res) -> chainCalls.incrementAndGet();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chainCalls.get()).isEqualTo(1);
    }

    @Test
    void oversizedBodyDoesNotThrowAndDegradesToIpOnlyKeying() throws Exception {
        AuthRateLimitFilter filter = newFilter(GENEROUS, 0L);
        String hugeEmail = "\"" + "a".repeat(9000) + "@test.com\"";
        String oversizedBody = "{\"email\":" + hugeEmail + ",\"password\":\"x\"}";
        assertThat(oversizedBody.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isGreaterThan(8192);
        MockHttpServletRequest request = jsonRequest("/api/auth/login", oversizedBody);
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (req, res) -> chainCalls.incrementAndGet();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(chainCalls.get()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    // --- UNIT-4/5: dual IP+email keying, independent dimensions ---

    private MockHttpServletRequest loginRequestFrom(String ip, String email) {
        MockHttpServletRequest request = jsonRequest("/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"x\"}");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void sameEmailFromTwoIpsTripsTheEmailDimension() throws Exception {
        // login-per-email = 1 (tight), login-per-ip = 1000 (generous) — only the email
        // dimension can plausibly trip here.
        AuthRateLimitProperties properties = new AuthRateLimitProperties(true, 1000, 1, 1000, 1000);
        AuthRateLimitFilter filter = newFilter(properties, 0L);
        FilterChain chain = (req, res) -> { };

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(loginRequestFrom("10.0.0.1", "shared@test.com"), first, chain);
        assertThat(first.getStatus()).isEqualTo(200);

        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(loginRequestFrom("10.0.0.2", "shared@test.com"), second, chain);

        assertThat(second.getStatus()).isEqualTo(429);
    }

    @Test
    void twoEmailsFromOneIpStillTripsTheIpDimension() throws Exception {
        // login-per-ip = 1 (tight), login-per-email = 1000 (generous) — dimensions are
        // independent: the IP ceiling trips even though neither email reached its own.
        AuthRateLimitProperties properties = new AuthRateLimitProperties(true, 1, 1000, 1000, 1000);
        AuthRateLimitFilter filter = newFilter(properties, 0L);
        FilterChain chain = (req, res) -> { };

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(loginRequestFrom("10.0.0.9", "one@test.com"), first, chain);
        assertThat(first.getStatus()).isEqualTo(200);

        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(loginRequestFrom("10.0.0.9", "two@test.com"), second, chain);

        assertThat(second.getStatus()).isEqualTo(429);
    }

    // --- Task 2.13: bounded eviction sweep (design "no scheduler" decision) ---
    // sdd-verify (Engram #6919) found this shipped GREEN with no RED and zero Jacoco
    // coverage. This test drives real key growth past MAX_TRACKED_KEYS (10_000) with a
    // stale clock, then advances the clock by one minute and confirms the very next
    // request triggers an inline sweep that evicts every stale bucket — proving the
    // "no scheduler" design decision actually bounds memory instead of growing forever.

    @SuppressWarnings("unchecked")
    private java.util.concurrent.ConcurrentHashMap<String, Object> bucketsOf(AuthRateLimitFilter filter) throws Exception {
        java.lang.reflect.Field field = AuthRateLimitFilter.class.getDeclaredField("buckets");
        field.setAccessible(true);
        return (java.util.concurrent.ConcurrentHashMap<String, Object>) field.get(filter);
    }

    @Test
    void sweepEvictsStaleBucketsOnceTrackedKeysExceedTheBoundAfterTheWindowRolls() throws Exception {
        AtomicReference<Clock> clockHolder = new AtomicReference<>(fixedClockAt(0L));
        AuthRateLimitFilter filter = newFilterWithMutableClock(GENEROUS, clockHolder);
        FilterChain chain = (req, res) -> { };

        // 10_001 distinct IP-only keys (no "email" field) at minute 0 — one over the
        // 10_000 sweep threshold, all now-stale once the clock advances.
        for (int i = 0; i <= 10_000; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setContent("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            request.setContentType("application/json");
            request.setRemoteAddr("10.0." + (i / 256) + "." + (i % 256));
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }
        assertThat(bucketsOf(filter)).hasSizeGreaterThan(10_000);

        clockHolder.set(fixedClockAt(61L));
        MockHttpServletRequest triggering = new MockHttpServletRequest("POST", "/api/auth/login");
        triggering.setContent("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        triggering.setContentType("application/json");
        triggering.setRemoteAddr("10.99.99.99");
        filter.doFilter(triggering, new MockHttpServletResponse(), chain);

        assertThat(bucketsOf(filter))
                .as("the sweep triggered by exceeding MAX_TRACKED_KEYS must evict every stale (minute 0) bucket")
                .hasSizeLessThan(10);
    }
}
