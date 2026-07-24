package com.tuhospedaje.service;

import com.tuhospedaje.dto.auth.AuthResponse;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;

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

    /**
     * Rotates the caller's refresh credential (Design ADR-2, PR1/WU2) and mints a new
     * access JWT from the owning user resolved via {@code Session.userId()}. Throws
     * {@link RefreshSessionService.Rejected} — with no further distinction — for an
     * invalid/expired/reused credential, an unknown owning user, AND when refresh
     * sessions are disabled entirely; this keeps the resulting HTTP 401 non-disclosing.
     */
    AuthResult refresh(String refreshCredential);
}
