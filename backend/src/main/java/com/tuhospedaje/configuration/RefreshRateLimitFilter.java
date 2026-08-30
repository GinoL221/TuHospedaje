package com.tuhospedaje.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.security.FixedWindowRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Path-scoped, IP-only fixed-window ceiling for {@code POST /api/auth/refresh}. See
 * design "Rate Limiting for POST /api/auth/refresh" for the full rationale.
 *
 * <p>Design "Decision: Duplicate the path-scoping hardening in RefreshRateLimitFilter":
 * the case-insensitive {@link PathPatternParser}, the bare+trailing-slash {@link
 * OrRequestMatcher}, and the repeated-slash normalization below are copied from {@link
 * AuthRateLimitFilter} rather than extracted, because that filter's test reads its
 * private {@code buckets} field by reflection and cannot be safely refactored in this
 * change. See the design doc's migration follow-up.
 *
 * <p>Unlike {@link AuthRateLimitFilter}, there is no request body to read here — the
 * refresh credential lives in an httpOnly cookie, not the body — so this filter never
 * wraps the request and never touches the input stream.
 */
@Component
@RequiredArgsConstructor
public class RefreshRateLimitFilter extends OncePerRequestFilter {

    private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

    static {
        PATH_PATTERN_PARSER.setCaseSensitive(false);
    }

    /** Collapses `//`, `///`, ... into a single `/` before matching (double-slash evasion). */
    private static final Pattern REPEATED_SLASHES = Pattern.compile("/{2,}");

    private static RequestMatcher postMatcherFor(String path) {
        return new OrRequestMatcher(
                PathPatternRequestMatcher.withPathPatternParser(PATH_PATTERN_PARSER).matcher(HttpMethod.POST, path),
                PathPatternRequestMatcher.withPathPatternParser(PATH_PATTERN_PARSER).matcher(HttpMethod.POST, path + "/")
        );
    }

    private final RequestMatcher matcher = postMatcherFor("/api/auth/refresh");

    private final SessionProperties properties;
    private final Supplier<Clock> clock;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter();

    /**
     * Design "Decision: Kill switch checked in shouldNotFilter()": the bean and its
     * SecurityConfig registration always exist; disabling is a pure no-op with zero
     * counting.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.rateLimit().enabled() || !matcher.matches(normalizePath(request));
    }

    private HttpServletRequest normalizePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return request;
        }
        String collapsed = REPEATED_SLASHES.matcher(uri).replaceAll("/");
        return collapsed.equals(uri) ? request : new NormalizedUriRequest(request, collapsed);
    }

    /** Presents a collapsed-slashes URI to the matcher without touching the real request. */
    private static final class NormalizedUriRequest extends HttpServletRequestWrapper {
        private final String normalizedUri;

        NormalizedUriRequest(HttpServletRequest request, String normalizedUri) {
            super(request);
            this.normalizedUri = normalizedUri;
        }

        @Override
        public String getRequestURI() {
            return normalizedUri;
        }
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Instant now = clock.get().instant();
        if (limiter.exceeds("refresh:ip:" + request.getRemoteAddr(), now,
                properties.rateLimit().refreshPerIpPerMinute())) {
            writeTooManyRequests(request, response, FixedWindowRateLimiter.retryAfterSeconds(now));
            return; // short-circuit: DB never touched
        }
        filterChain.doFilter(request, response); // no wrapper — the body is never consumed
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response,
                                       long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "error", messageSource.getMessage("error.rate_limit", null, request.getLocale()),
                "status", 429));
    }
}
