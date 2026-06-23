package com.tuhospedaje.auth;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.dto.auth.AuthResponse;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.AuthService;
import com.tuhospedaje.service.AuthService.AuthResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthServiceImplTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

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
}
