package com.tuhospedaje.controller;

import com.tuhospedaje.configuration.AuthCookieFactory;
import com.tuhospedaje.configuration.RefreshCookieFactory;
import com.tuhospedaje.dto.auth.AuthResponse;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.service.AuthService;
import com.tuhospedaje.service.AuthService.AuthResult;
import com.tuhospedaje.service.RefreshSessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication, registration, and session identity")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";

    private final AuthService authService;
    private final AuthCookieFactory authCookieFactory;
    private final RefreshCookieFactory refreshCookieFactory;
    private final CsrfTokenRepository csrfTokenRepository;
    private final MessageSource messageSource;

    @Operation(summary = "Register a new user", description = "Creates a new user account and sets the ACCESS_TOKEN session cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or malformed fields", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email address is already registered", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthResult result = authService.register(request);
        rotateCsrfToken(httpRequest, httpResponse);
        return withAccessTokenCookie(HttpStatus.CREATED, result);
    }

    @Operation(summary = "Authenticate a user", description = "Validates credentials and sets the ACCESS_TOKEN session cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or malformed fields", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthResult result = authService.login(request);
        rotateCsrfToken(httpRequest, httpResponse);
        return withAccessTokenCookie(HttpStatus.OK, result);
    }

    @Operation(summary = "Log out", description = "Clears the ACCESS_TOKEN session cookie. Idempotent: "
            + "succeeds even without a prior session, so it can always be called safely.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "ACCESS_TOKEN cookie cleared (no-op if there was no session)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing/invalid CSRF token", content = @Content)
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie clearingCookie = authCookieFactory.buildClearingCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearingCookie.toString())
                .build();
    }

    @Operation(summary = "Get the current session's user", description = "Resolves the authenticated user's claims from the ACCESS_TOKEN cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session is valid",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "No valid session", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        AuthResponse response = authService.currentUser(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Bootstrap the browser CSRF token")
    @ApiResponse(responseCode = "204", description = "Fresh readable XSRF-TOKEN cookie materialized")
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rotate the refresh session", description = "Exchanges the REFRESH_TOKEN cookie "
            + "for a new ACCESS_TOKEN and a rotated REFRESH_TOKEN. CSRF-exempt: the httpOnly refresh "
            + "cookie itself is the credential, and it is never readable cross-origin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh succeeded; both cookies rotated",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, expired, or reused refresh credential", content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpRequest) {
        String refreshCredential = resolveRefreshToken(httpRequest);
        if (refreshCredential == null) {
            throw new RefreshSessionService.Rejected();
        }
        AuthResult result = authService.refresh(refreshCredential);
        return withAccessTokenCookie(HttpStatus.OK, result);
    }

    /**
     * Non-disclosing (Delta Spec: "Invalid/missing/reused refresh token rejected
     * generically") — every rejection reason (missing cookie, malformed, expired,
     * revoked, reused, unknown user, refresh sessions disabled) maps to this SAME 401
     * body and clears the now-useless REFRESH_TOKEN cookie so a dead credential doesn't
     * linger in the browser.
     */
    @ExceptionHandler(RefreshSessionService.Rejected.class)
    public ResponseEntity<Map<String, Object>> handleRefreshRejected(Locale locale) {
        ResponseCookie clearingRefreshCookie = refreshCookieFactory.buildClearingRefreshCookie();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, clearingRefreshCookie.toString())
                .body(Map.of("error", messageSource.getMessage("error.session.refresh_invalid", null, locale), "status", 401));
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // SecurityConfig disables Spring's default per-request CsrfAuthenticationStrategy (it
    // rotated the XSRF-TOKEN cookie on every authenticated request, not just login, which
    // raced against concurrent requests — see the comment there). But a token issued before
    // authentication must still not remain valid afterward, or an attacker able to plant a
    // cookie value for this origin pre-login could replay it post-login.
    //
    // Generate-and-save a replacement in this one call (never just clear) so the response
    // never leaves the client without a usable cookie: the frontend's GET /api/auth/csrf
    // bootstrap that follows is a separate, un-retried request, and if it's interrupted
    // (network blip, navigation), a client left with only a cleared cookie would be
    // authenticated but unable to make any CSRF-protected request, including logout, until
    // an unrelated page reload. (Clearing first via a second saveToken(null, ...) call would
    // add a second Set-Cookie for the same name in this response — redundant, and some
    // response-cookie readers only see the first one — so this only ever writes one.)
    private void rotateCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        if (csrfTokenRepository.loadToken(request) == null) {
            return;
        }
        log.debug("Rotating CSRF token on authentication");
        CsrfToken freshToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(freshToken, request, response);
    }

    private ResponseEntity<AuthResponse> withAccessTokenCookie(HttpStatus status, AuthResult result) {
        ResponseCookie accessTokenCookie = authCookieFactory.buildAccessTokenCookie(result.token());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        // refreshCredential is null when refresh sessions are disabled (Design ADR-0
        // kill-switch) — login/register/refresh all degrade to ACCESS_TOKEN-only.
        if (result.refreshCredential() != null) {
            ResponseCookie refreshTokenCookie = refreshCookieFactory.buildRefreshCookie(result.refreshCredential());
            response.header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        }
        return response.body(result.body());
    }
}
