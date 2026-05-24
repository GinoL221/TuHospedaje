package com.tuhospedaje.auth;

import com.tuhospedaje.dto.auth.AuthResponse;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
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
        AuthResponse response = authService.register(request);
        assertThat(response.getToken()).isNotBlank();
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
        AuthResponse response = authService.login(new LoginRequest("juan@test.com", "123456"));
        assertThat(response.getToken()).isNotBlank();
    }
    @Test
    void shouldThrowOnInvalidCredentials() {
        assertThrows(Exception.class,
                () -> authService.login(new LoginRequest("noexiste@test.com", "pass")));
    }
}
