package com.tuhospedaje.service;

import com.tuhospedaje.dto.auth.AuthResponse;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.service.EmailOutboxService.WelcomeResendResult;

public interface AuthService {

    /**
     * Carries the claims body (for the JSON response) alongside the raw signed JWT (for
     * the controller to attach as the {@code ACCESS_TOKEN} cookie), and the raw refresh
     * credential (for the controller to attach as the {@code REFRESH_TOKEN} cookie).
     * {@code refreshCredential} is {@code null} when refresh sessions are disabled
     * (Design ADR-0 kill-switch). Neither token nor refreshCredential ever leave this
     * carrier into the JSON body.
     */
    record AuthResult(AuthResponse body, String token, String refreshCredential) {
    }

    AuthResult register(RegisterRequest request);
    AuthResult login(LoginRequest request);

    /**
     * Resolves the current session's claims from the authenticated principal's email
     * (used by {@code GET /api/auth/me}). No token is generated or returned here.
     */
    AuthResponse currentUser(String email);

    WelcomeResendResult resendWelcome(String email);

    /**
     * Rotates the caller's refresh credential (Design ADR-2, PR1/WU2) and mints a new
     * access JWT from the owning user resolved via {@code Session.userId()}. Throws
     * {@link RefreshSessionService.Rejected} — with no further distinction — for an
     * invalid/expired/reused credential, an unknown owning user, AND when refresh
     * sessions are disabled entirely; this keeps the resulting HTTP 401 non-disclosing.
     */
    AuthResult refresh(String refreshCredential);

    /**
     * Verifies {@code currentPassword} against the stored hash for {@code email}, and if
     * it matches, encodes and persists {@code newPassword}, then revokes every refresh
     * session family for that user (Design PR3b, {@code reason="PASSWORD_CHANGE"}) —
     * best-effort via {@link RefreshSessionService} ADR-0's {@code ObjectProvider}, a
     * no-op when refresh sessions are disabled. Throws {@link IllegalArgumentException}
     * (mapped to HTTP 400 by {@code GlobalExceptionHandler}, matching the existing
     * {@code login}/{@code register} convention) when the current password does not
     * match — no session is revoked and no event is persisted in that case.
     */
    void changePassword(String email, String currentPassword, String newPassword);

    /**
     * Revokes ONLY the calling device's refresh family (Design PR3/WU4,
     * {@code revokeCurrent} — never {@code revokeAll}): other devices' sessions stay
     * valid (Delta Spec: "Only the calling device is logged out"). Best-effort via
     * {@link RefreshSessionService} ADR-0's {@code ObjectProvider} — a no-op when
     * refresh sessions are disabled or {@code refreshCredential} is {@code null} (no
     * REFRESH_TOKEN cookie was ever presented). {@link RefreshSessionService.Rejected}
     * (already-consumed/unknown/reused credential) is deliberately swallowed here so
     * logout stays idempotent/204 regardless of the credential's state.
     */
    void logout(String refreshCredential);
}
