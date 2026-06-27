package com.tuhospedaje.service;

import com.tuhospedaje.dto.auth.AuthResponse;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;

public interface AuthService {

    /**
     * Carries the claims body (for the JSON response) alongside the raw signed JWT (for
     * the controller to attach as the {@code ACCESS_TOKEN} cookie). The token never
     * leaves this carrier into the JSON body.
     */
    record AuthResult(AuthResponse body, String token) {
    }

    AuthResult register(RegisterRequest request);
    AuthResult login(LoginRequest request);

    /**
     * Resolves the current session's claims from the authenticated principal's email
     * (used by {@code GET /api/auth/me}). No token is generated or returned here.
     */
    AuthResponse currentUser(String email);
}
