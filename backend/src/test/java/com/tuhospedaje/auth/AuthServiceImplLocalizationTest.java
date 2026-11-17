package com.tuhospedaje.auth;

import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.exception.DuplicateEmailException;
import com.tuhospedaje.exception.LocalizedIllegalArgumentException;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.EmailOutboxService;
import com.tuhospedaje.service.RefreshSessionService;
import com.tuhospedaje.service.impl.AuthServiceImpl;
import com.tuhospedaje.service.impl.RegistrationPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplLocalizationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RegistrationPersistenceService registrationPersistenceService;

    @Mock
    private EmailOutboxService emailOutboxService;

    @Mock
    private ObjectProvider<RefreshSessionService> refreshSessions;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                registrationPersistenceService,
                emailOutboxService,
                refreshSessions);
    }

    @Test
    void registerThrowsKeyCarryingDuplicateEmailOnPreCheckAndRace() {
        User existing = User.builder().id(1L).email("dup@test.com").build();
        when(userRepository.findByEmail("dup@test.com")).thenReturn(Optional.of(existing));

        DuplicateEmailException preCheckEx = assertThrows(
                DuplicateEmailException.class,
                () -> authService.register(new RegisterRequest("Ana", "Gomez", "dup@test.com", "Pass1234")));
        assertThat(preCheckEx.getMessage()).isEqualTo("error.auth.duplicate_email");

        when(userRepository.findByEmail("race@test.com"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("Pass1234")).thenReturn("encoded");
        when(registrationPersistenceService.persist(any(), any())).thenThrow(new RuntimeException("unique"));

        DuplicateEmailException raceEx = assertThrows(
                DuplicateEmailException.class,
                () -> authService.register(new RegisterRequest("Ana", "Gomez", "race@test.com", "Pass1234")));
        assertThat(raceEx.getMessage()).isEqualTo("error.auth.duplicate_email");
    }

    @Test
    void loginWhenUserMissingAfterAuthenticationThrowsLocalizedInvalidKey() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        LocalizedIllegalArgumentException ex = assertThrows(
                LocalizedIllegalArgumentException.class,
                () -> authService.login(new LoginRequest("ghost@test.com", "Pass1234")));
        assertThat(ex.getErrorCode()).isEqualTo("error.auth.invalid");
    }

    @Test
    void currentUserResendWelcomeAndChangePasswordThrowLocalizedKeysWhenUserMissing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        when(userRepository.lockByEmail("missing@test.com")).thenReturn(Optional.empty());

        LocalizedIllegalArgumentException currentUserEx = assertThrows(
                LocalizedIllegalArgumentException.class,
                () -> authService.currentUser("missing@test.com"));
        assertThat(currentUserEx.getErrorCode()).isEqualTo("error.user.not_found");

        LocalizedIllegalArgumentException resendEx = assertThrows(
                LocalizedIllegalArgumentException.class,
                () -> authService.resendWelcome("missing@test.com"));
        assertThat(resendEx.getErrorCode()).isEqualTo("error.user.not_found");

        LocalizedIllegalArgumentException changePwEx = assertThrows(
                LocalizedIllegalArgumentException.class,
                () -> authService.changePassword("missing@test.com", "old", "new"));
        assertThat(changePwEx.getErrorCode()).isEqualTo("error.user.not_found");
    }

    @Test
    void changePasswordThrowsLocalizedKeyWhenCurrentPasswordDoesNotMatch() {
        User user = User.builder().id(1L).email("user@test.com").password("encoded-real").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-real")).thenReturn(false);

        LocalizedIllegalArgumentException ex = assertThrows(
                LocalizedIllegalArgumentException.class,
                () -> authService.changePassword("user@test.com", "wrong", "newPass1"));
        assertThat(ex.getErrorCode()).isEqualTo("error.auth.current_password.incorrect");
    }
}
