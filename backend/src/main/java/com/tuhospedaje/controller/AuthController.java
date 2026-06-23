package com.tuhospedaje.controller;

import com.tuhospedaje.configuration.AuthCookieFactory;
import com.tuhospedaje.dto.auth.AuthResponse;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.service.AuthService;
import com.tuhospedaje.service.AuthService.AuthResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication, registration, and session identity")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieFactory authCookieFactory;

    @Operation(summary = "Register a new user", description = "Creates a new user account and sets the ACCESS_TOKEN session cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error — missing or malformed fields", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email address is already registered", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = authService.register(request);
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request);
        return withAccessTokenCookie(HttpStatus.OK, result);
    }

    @Operation(summary = "Log out", description = "Clears the ACCESS_TOKEN session cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout successful — ACCESS_TOKEN cookie cleared", content = @Content),
            @ApiResponse(responseCode = "401", description = "No valid session", content = @Content),
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

    private ResponseEntity<AuthResponse> withAccessTokenCookie(HttpStatus status, AuthResult result) {
        ResponseCookie accessTokenCookie = authCookieFactory.buildAccessTokenCookie(result.token());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .body(result.body());
    }
}
