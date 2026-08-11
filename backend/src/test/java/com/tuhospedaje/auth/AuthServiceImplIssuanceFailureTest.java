package com.tuhospedaje.auth;

import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.AuthService.AuthResult;
import com.tuhospedaje.service.EmailOutboxService;
import com.tuhospedaje.service.RefreshSessionService;
import com.tuhospedaje.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure-mock unit coverage for the failure mode a full-Spring-context test can't easily force:
 * refresh-session issuance is a best-effort enhancement, so a transient failure there must
 * degrade to an access-token-only result, never fail registration/login outright.
 */
class AuthServiceImplIssuanceFailureTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtService jwtService = mock(JwtService.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<RefreshSessionService> refreshSessions = mock(ObjectProvider.class);
    private final RefreshSessionService sessions = mock(RefreshSessionService.class);

    private final AuthServiceImpl authService = new AuthServiceImpl(
            userRepository,
            mock(PasswordEncoder.class),
            jwtService,
            mock(AuthenticationManager.class),
            mock(EmailOutboxService.class),
            refreshSessions);

    @Test
    void registerSucceedsWithAnAccessTokenOnlyWhenRefreshSessionIssuanceFails() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(jwtService.generateToken(any(), any())).thenReturn("access-token-123");
        when(refreshSessions.getIfAvailable()).thenReturn(sessions);
        when(sessions.issue(any(User.class))).thenThrow(new RuntimeException("refresh_token_families insert failed"));

        AuthResult result = authService.register(
                new RegisterRequest("Ada", "Lovelace", "ada-issuance-failure@test.com", "123456"));

        assertThat(result.token()).isEqualTo("access-token-123");
        assertThat(result.refreshCredential()).isNull();
    }
}
