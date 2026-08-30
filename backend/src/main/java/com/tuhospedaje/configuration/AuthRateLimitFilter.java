package com.tuhospedaje.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
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
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Path-scoped, fixed-window attempt ceiling for {@code POST /api/auth/login} and
 * {@code POST /api/auth/register}. See design "Rate Limiting for /api/auth/login and
 * /api/auth/register" for the full rationale behind every decision below.
 *
 * <p>Mirrors {@link JwtAuthenticationFilter}: same package, same base class, same
 * {@code @RequiredArgsConstructor} DI style.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    /**
     * Threat Matrix (design doc): a stock case-sensitive {@link PathPatternParser} still
     * lets {@code /api/auth/Login} slip past {@link #shouldNotFilter}, so this filter
     * configures its own case-insensitive parser instance instead of
     * {@link PathPatternRequestMatcher#withDefaults()}. This does not affect
     * {@code SecurityConfig}'s own authorization matchers, which use a separate parser
     * instance. Trailing-slash tolerance ({@code /api/auth/login/}) is handled below by
     * matching both the bare and trailing-slash pattern explicitly, per Spring's own
     * guidance since {@code PathPatternParser#setMatchOptionalTrailingSeparator} was
     * deprecated in 6.0 (transparent trailing-slash matching was deprecated in favor of
     * explicit patterns).
     */
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

    /**
     * Design "Decision: Path scoping via PathPatternRequestMatcher, not
     * getServletPath()": using the identical matcher type SecurityConfig uses for its
     * own authorization decisions removes a whole class of silent-bypass divergence
     * between how Spring Security resolves a path and how a raw string compare would.
     */
    private final RequestMatcher loginMatcher = postMatcherFor("/api/auth/login");
    private final RequestMatcher registerMatcher = postMatcherFor("/api/auth/register");
    private final RequestMatcher matcher = new OrRequestMatcher(loginMatcher, registerMatcher);

    private final AuthRateLimitProperties properties;
    private final Supplier<Clock> clock;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    /**
     * Design "Decision: Fixed window via atomic compute, lazy bounded sweep, no
     * scheduler": {@code compute} is atomic per key under {@link ConcurrentHashMap}, so
     * the read-reset-increment sequence needs no extra lock and cannot race.
     */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicBoolean sweeping = new AtomicBoolean(false);

    private static final int MAX_TRACKED_KEYS = 10_000;

    /**
     * Design "the size cap prevents an attacker from forcing unbounded buffering; oversized
     * bodies degrade to IP-only keying, never to bypass".
     */
    private static final int MAX_BODY_BYTES = 8192;

    private record Bucket(long minute, int count) {
    }

    /**
     * Design "Decision: Kill switch checked in shouldNotFilter(), not
     * @ConditionalOnProperty": the bean and its SecurityConfig registration always
     * exist; disabling is a pure no-op with zero counting and zero body buffering.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.enabled() || !matcher.matches(normalizePath(request));
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
        boolean isLogin = loginMatcher.matches(normalizePath(request));
        String namespace = isLogin ? "login" : "register";
        int ipLimit = isLogin ? properties.loginPerIpPerMinute() : properties.registerPerIpPerMinute();
        int emailLimit = isLogin ? properties.loginPerEmailPerMinute() : properties.registerPerEmailPerMinute();

        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        CachedBodyRequest wrapped = new CachedBodyRequest(request, body);
        String emailKey = extractEmailKey(body);

        long minute = currentMinute();
        String ipKey = namespace + ":ip:" + request.getRemoteAddr();

        // Design "Data Flow": both dimensions are evaluated; either alone can trip.
        // Counters increment on every attempt regardless of outcome — both `exceeds`
        // calls must run (no short-circuit skip) so a blocked email attempt still
        // consumes its IP-dimension quota, and vice versa.
        boolean ipExceeded = exceeds(ipKey, minute, ipLimit);
        boolean emailExceeded = emailKey != null
                && exceeds(namespace + ":email:" + emailKey, minute, emailLimit);

        if (ipExceeded || emailExceeded) {
            writeTooManyRequests(request, response);
            return;
        }

        filterChain.doFilter(wrapped, response);
    }

    /**
     * Design interface contract "Email extraction (never throws, never 4xx from the
     * filter, never touches the DB)". Malformed/empty/oversized bodies fall back to
     * IP-only keying; the controller downstream still returns its normal 400 via
     * Jackson deserializing the re-readable body.
     */
    private String extractEmailKey(byte[] body) {
        if (body.length == 0 || body.length > MAX_BODY_BYTES) {
            return null;
        }
        try {
            String raw = objectMapper.readTree(body).path("email").asText(null);
            if (raw != null && !raw.isBlank()) {
                return raw.trim().toLowerCase(Locale.ROOT);
            }
        } catch (IOException ignored) {
            // malformed -> IP-only key; controller still returns 400
        }
        return null;
    }

    /**
     * Design "Decision: Hand-rolled re-readable body wrapper, not
     * ContentCachingRequestWrapper" — that class caches bytes as downstream consumers
     * read them, it does not make the stream re-readable; draining it here to extract
     * the email would leave Jackson seeing EOF downstream and break every login/register.
     * {@code getInputStream()}/{@code getReader()} each return a FRESH view over the
     * buffered bytes on every call.
     */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
        }
    }

    private static final class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream buffer;

        CachedBodyServletInputStream(byte[] body) {
            this.buffer = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // No async read support needed — this stream is always fully buffered.
        }

        @Override
        public int read() {
            return buffer.read();
        }
    }

    private long currentMinute() {
        return clock.get().instant().getEpochSecond() / 60;
    }

    /**
     * Design interface contract: atomic per-key compute; a stale bucket for the current
     * key resets itself on the next hit for that key, no extra lock required.
     */
    private boolean exceeds(String key, long minute, int limit) {
        maybeSweep(minute);
        Bucket updated = buckets.compute(key, (k, prev) ->
                (prev == null || prev.minute() != minute)
                        ? new Bucket(minute, 1)
                        : new Bucket(minute, prev.count() + 1));
        return updated.count() > limit;
    }

    /**
     * Design "lazy bounded sweep, no scheduler": abandoned keys are swept inline when
     * the map grows past {@link #MAX_TRACKED_KEYS}, guarded by an {@link AtomicBoolean}
     * so exactly one thread performs the sweep.
     */
    private void maybeSweep(long minute) {
        if (buckets.size() > MAX_TRACKED_KEYS && sweeping.compareAndSet(false, true)) {
            try {
                buckets.entrySet().removeIf(e -> e.getValue().minute() < minute);
            } finally {
                sweeping.set(false);
            }
        }
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long retryAfterSeconds = Math.max(1, 60 - (clock.get().instant().getEpochSecond() % 60));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "error", messageSource.getMessage("error.rate_limit", null, request.getLocale()),
                "status", 429));
    }
}
