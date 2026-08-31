package com.tuhospedaje.configuration;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

/**
 * Design: "RefreshRateLimitFilter" — path-scoped, IP-only fixed-window ceiling for
 * {@code POST /api/auth/refresh}, built on the extracted {@link
 * com.tuhospedaje.security.FixedWindowRateLimiter}. Mirrors {@link
 * AuthRateLimitFilterTest}'s style (fixed/mutable {@link Clock}, {@code
 * shouldNotFilter} protected-method access from the same package), but there is no
 * body to read — only IP keying.
 */
@ExtendWith(MockitoExtension.class)
class RefreshRateLimitFilterTest {

    @Mock
    private MessageSource messageSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void stubRateLimitMessage() {
        lenient().when(messageSource.getMessage(eq("error.rate_limit"), any(), any(Locale.class)))
                .thenReturn("Too many attempts. Try again later.");
    }

    private SessionProperties propertiesWith(boolean enabled, int refreshPerIpPerMinute) {
        return new SessionProperties(
                Duration.ofMinutes(15),
                new SessionProperties.RefreshProperties(true, Duration.ofDays(30), Duration.ofSeconds(5)),
                new SessionProperties.CleanupProperties(Duration.ofDays(1), 100),
                new SessionProperties.RateLimitProperties(enabled, 10, refreshPerIpPerMinute));
    }

    private static final SessionProperties GENEROUS = new SessionProperties(
            Duration.ofMinutes(15),
            new SessionProperties.RefreshProperties(true, Duration.ofDays(30), Duration.ofSeconds(5)),
            new SessionProperties.CleanupProperties(Duration.ofDays(1), 100),
            new SessionProperties.RateLimitProperties(true, 10, 1000));

    private Supplier<Clock> fixedClock(long epochSecond) {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
        return () -> clock;
    }

    private RefreshRateLimitFilter newFilter(SessionProperties properties, long epochSecond) {
        return new RefreshRateLimitFilter(properties, fixedClock(epochSecond), objectMapper, messageSource);
    }

    private RefreshRateLimitFilter newFilterWithMutableClock(SessionProperties properties,
                                                              AtomicReference<Clock> clockHolder) {
        return new RefreshRateLimitFilter(properties, clockHolder::get, objectMapper, messageSource);
    }

    private Clock fixedClockAt(long epochSecond) {
        return Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
    }

    private MockHttpServletRequest refreshRequest() {
        return new MockHttpServletRequest("POST", "/api/auth/refresh");
    }

    // --- F-1/F-2/F-3: fixed-window counting ---

    @Test
    void nthRequestAtTheLimitStillPassesThrough() throws Exception {
        AtomicReference<Clock> clockHolder = new AtomicReference<>(fixedClockAt(0L));
        RefreshRateLimitFilter filter = newFilterWithMutableClock(propertiesWith(true, 2), clockHolder);
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (req, res) -> chainCalls.incrementAndGet();

        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(refreshRequest(), response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        assertThat(chainCalls.get()).isEqualTo(2);
    }

    @Test
    void limitPlusOneSameMinuteReturns429AndDoesNotInvokeChain() throws Exception {
        AtomicReference<Clock> clockHolder = new AtomicReference<>(fixedClockAt(0L));
        RefreshRateLimitFilter filter = newFilterWithMutableClock(propertiesWith(true, 2), clockHolder);
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (req, res) -> chainCalls.incrementAndGet();

        for (int i = 0; i < 2; i++) {
            filter.doFilter(refreshRequest(), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(refreshRequest(), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
        assertThat(chainCalls.get()).isEqualTo(2);
    }

    @Test
    void windowResetAfterClockAdvancesPastTheMinuteBoundary() throws Exception {
        AtomicReference<Clock> clockHolder = new AtomicReference<>(fixedClockAt(0L));
        RefreshRateLimitFilter filter = newFilterWithMutableClock(propertiesWith(true, 2), clockHolder);
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (req, res) -> chainCalls.incrementAndGet();

        for (int i = 0; i < 2; i++) {
            filter.doFilter(refreshRequest(), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(refreshRequest(), blocked, chain);
        assertThat(blocked.getStatus()).isEqualTo(429);

        clockHolder.set(fixedClockAt(61L));
        MockHttpServletResponse afterReset = new MockHttpServletResponse();
        filter.doFilter(refreshRequest(), afterReset, chain);

        assertThat(afterReset.getStatus()).isEqualTo(200);
        assertThat(chainCalls.get()).isEqualTo(3);
    }

    // --- F-4: two IPs key independently ---

    @Test
    void twoIpsKeyIndependently() throws Exception {
        RefreshRateLimitFilter filter = newFilter(propertiesWith(true, 1), 0L);
        FilterChain chain = (req, res) -> { };

        MockHttpServletRequest firstIpRequest = refreshRequest();
        firstIpRequest.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(firstIpRequest, first, chain);
        assertThat(first.getStatus()).isEqualTo(200);

        MockHttpServletRequest secondIpRequest = refreshRequest();
        secondIpRequest.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(secondIpRequest, second, chain);
        assertThat(second.getStatus()).isEqualTo(200);

        MockHttpServletRequest firstIpAgain = refreshRequest();
        firstIpAgain.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(firstIpAgain, blocked, chain);
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    // --- F-5: kill switch ---

    @Test
    void disabledKillSwitchSkipsFilteringEntirely() throws Exception {
        RefreshRateLimitFilter filter = newFilter(propertiesWith(false, 1), 0L);

        assertThat(filter.shouldNotFilter(refreshRequest())).isTrue();

        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest request = refreshRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    // --- F-6: scoping matrix — only POST /api/auth/refresh is in scope ---

    @ParameterizedTest
    @CsvSource({
            "POST,/api/auth/login",
            "POST,/api/auth/register",
            "POST,/api/auth/logout",
            "POST,/api/auth/welcome-email/resend",
            "GET,/api/auth/csrf",
            "GET,/api/auth/me",
            "GET,/api/auth/refresh"
    })
    void skipsEveryPathOtherThanPostRefresh(String method, String path) {
        RefreshRateLimitFilter filter = newFilter(GENEROUS, 0L);
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    // --- F-7: path-normalization/evasion matrix (Threat Matrix) ---
    // Failure mode to prevent: a URI that Spring Security's authorization resolves to
    // /api/auth/refresh (thus permitAll() and routed to the handler) while this
    // filter's matcher does not — an unlimited refresh path.

    @ParameterizedTest
    @CsvSource({
            "/api/auth/refresh/",
            "/api/auth//refresh",
            "/api/auth/Refresh",
            "/api/AUTH/refresh",
            "/api/auth/refresh;jsessionid=x",
            "/api/auth/%72efresh"
    })
    void isRateLimitedForEveryNormalizationVariantOfRefreshPath(String path) {
        RefreshRateLimitFilter filter = newFilter(GENEROUS, 0L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        assertThat(filter.shouldNotFilter(request))
                .as("path '%s' must not bypass rate limiting via normalization/encoding", path)
                .isFalse();
    }

    // --- F-8: 429 body shape is exactly {error,status} ---

    @Test
    void the429BodyContainsExactlyErrorAndStatusKeys() throws Exception {
        RefreshRateLimitFilter filter = newFilter(propertiesWith(true, 1), 0L);
        FilterChain chain = (req, res) -> { };

        filter.doFilter(refreshRequest(), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(refreshRequest(), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        JsonNode body = objectMapper.readTree(blocked.getContentAsByteArray());
        assertThat(body.properties()).extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("error", "status");
        assertThat(body.get("status").asInt()).isEqualTo(429);
        assertThat(body.toString()).doesNotContain("\"token\"");
    }

    // --- F-9: the body is never consumed — no wrapper is needed ---

    @Test
    void requestBodyIsNeverConsumedByTheFilter() throws Exception {
        RefreshRateLimitFilter filter = newFilter(GENEROUS, 0L);
        MockHttpServletRequest request = refreshRequest();
        String originalBody = "irrelevant-refresh-body";
        request.setContent(originalBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        AtomicReference<jakarta.servlet.http.HttpServletRequest> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set((jakarta.servlet.http.HttpServletRequest) req);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        jakarta.servlet.http.HttpServletRequest downstream = captured.get();
        assertThat(downstream).isNotNull();
        String viaInputStream = new String(downstream.getInputStream().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        assertThat(viaInputStream)
                .as("the filter must never read the request body — the chain must still see an "
                        + "unread input stream")
                .isEqualTo(originalBody);
    }
}
