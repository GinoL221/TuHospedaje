package com.tuhospedaje.auth;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.dto.auth.AuthResponse;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.service.AuthService;
import com.tuhospedaje.service.AuthService.AuthResult;
import com.tuhospedaje.service.EmailOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthServiceImplTest {

    @Autowired
    private AuthService authService;

    @MockitoBean
    private EmailOutboxService emailOutboxService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest("Juan", "Pérez", "juan@test.com", "123456");
        AuthResult result = authService.register(request);
        assertThat(result.token()).isNotBlank();
        assertThat(result.body().getEmail()).isEqualTo("juan@test.com");
        assertThat(result.body().getFirstName()).isEqualTo("Juan");
    }
    @Test
    void shouldThrowOnDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Juan", " Pérez", "juan@test.com", "123456");
        authService.register(request);
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }
    @Test
    void shouldLoginSuccessfully() {
        authService.register(new RegisterRequest("Juan", "Pérez", "juan@test.com", "123456"));
        AuthResult result = authService.login(new LoginRequest("juan@test.com", "123456"));
        assertThat(result.token()).isNotBlank();
        assertThat(result.body().getEmail()).isEqualTo("juan@test.com");
    }
    @Test
    void shouldThrowOnInvalidCredentials() {
        assertThrows(Exception.class,
                () -> authService.login(new LoginRequest("noexiste@test.com", "pass")));
    }
    @Test
    void shouldReturnCurrentUserClaimsWithoutToken() {
        authService.register(new RegisterRequest("Juan", "Pérez", "juan@test.com", "123456"));
        AuthResponse response = authService.currentUser("juan@test.com");
        assertThat(response.getEmail()).isEqualTo("juan@test.com");
        assertThat(response.getFirstName()).isEqualTo("Juan");
    }
    @Test
    void shouldThrowWhenCurrentUserNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.currentUser("noexiste@test.com"));
    }

    // ADR-0 (conditional-bean kill-switch): this test class runs with the DEFAULT test
    // properties, where app.session.refresh.enabled=false, so no RefreshSessionService
    // bean exists. If AuthServiceImpl depended on it as a hard constructor dependency
    // instead of ObjectProvider<RefreshSessionService>, the whole Spring context above
    // would fail to start and EVERY test in this class (and BackendApplicationTests,
    // AuthControllerIntegrationTest, etc.) would fail before this assertion ever ran.
    // The context starting AND refreshCredential() being null together prove the
    // kill-switch: register/login degrade gracefully with refresh sessions disabled.
    @Test
    void refreshCredentialIsNullWhenRefreshSessionsAreDisabled() {
        RegisterRequest request = new RegisterRequest("Juan", "Pérez", "juan-refresh-off@test.com", "123456");
        AuthResult result = authService.register(request);
        assertThat(result.refreshCredential()).isNull();
    }

    @Test
    void shouldEnqueueWelcomeEmailOnRegister() {
        RegisterRequest request = new RegisterRequest("Juan", "Pérez", "juan-welcome@test.com", "123456");
        authService.register(request);

        verify(emailOutboxService).enqueueWelcome(org.mockito.ArgumentMatchers.any(), eq(request));
    }
}
