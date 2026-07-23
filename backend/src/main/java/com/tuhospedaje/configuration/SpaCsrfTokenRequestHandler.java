package com.tuhospedaje.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       Supplier<CsrfToken> csrfToken) {
        xor.handle(request, response, csrfToken);
        if (!isUnauthenticatedBootstrap(request)) {
            csrfToken.get();
        }
        // For every endpoint other than /api/auth/csrf, csrfToken.get() above already
        // materializes the token eagerly on every response (matching the eager behavior
        // the deleted CsrfCookieFilter used to provide). For /api/auth/csrf specifically,
        // real materialization instead happens in AuthController.csrf() via its own
        // explicit csrfToken.getToken() call, downstream of SecurityConfig's .authenticated()
        // gate — see the note on isUnauthenticatedBootstrap() below for why that decision
        // can't be made here.
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        CsrfTokenRequestHandler handler = StringUtils.hasText(headerValue) ? plain : xor;
        return handler.resolveCsrfTokenValue(request, csrfToken);
    }

    // NOTE: this authentication check is unreachable in practice. Spring Security's
    // CsrfFilter (which calls handle() above) runs before JwtAuthenticationFilter in the
    // default filter chain — JwtAuthenticationFilter is only registered relative to
    // UsernamePasswordAuthenticationFilter (see SecurityConfig#securityFilterChain,
    // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)), which
    // itself runs after CsrfFilter. So SecurityContextHolder is always unauthenticated
    // here for a /api/auth/csrf request, authenticated or not, and this method always
    // returns true for that URI. That's harmless ONLY because the real authentication
    // check for this endpoint lives in SecurityConfig's
    // .requestMatchers(HttpMethod.GET, "/api/auth/csrf").authenticated() (enforced later
    // by the authorization filter) plus AuthController.csrf()'s own explicit
    // csrfToken.getToken() call. Do not remove that controller call assuming this method
    // already gates on real authentication — it does not, and doing so would silently
    // reintroduce the original post-login CSRF cookie bug this class was written to fix.
    private boolean isUnauthenticatedBootstrap(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return "/api/auth/csrf".equals(request.getRequestURI())
                && (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken);
    }
}
