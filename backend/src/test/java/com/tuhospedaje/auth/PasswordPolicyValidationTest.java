package com.tuhospedaje.auth;

import com.tuhospedaje.dto.auth.PasswordChangeRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private RegisterRequest validRegisterRequest(String password) {
        return RegisterRequest.builder()
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan@test.com")
                .password(password)
                .build();
    }

    private PasswordChangeRequest validPasswordChangeRequest(String newPassword) {
        return PasswordChangeRequest.builder()
                .currentPassword("OldPass1")
                .newPassword(newPassword)
                .build();
    }

    @Test
    void registerRequest_acceptsValidPassword() {
        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(validRegisterRequest("Pass1234"));
        assertThat(violations).isEmpty();
    }

    @Test
    void registerRequest_rejectsPasswordWithoutUppercase() {
        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(validRegisterRequest("pass1234"));
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void registerRequest_rejectsPasswordWithoutDigit() {
        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(validRegisterRequest("Password"));
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void registerRequest_rejectsPasswordShorterThan6Chars() {
        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(validRegisterRequest("Pass1"));
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void registerRequest_rejectsPasswordLongerThan20Chars() {
        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(validRegisterRequest("Pass12345678901234567"));
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void passwordChangeRequest_acceptsValidNewPassword() {
        Set<ConstraintViolation<PasswordChangeRequest>> violations =
                validator.validate(validPasswordChangeRequest("NewPass12"));
        assertThat(violations).isEmpty();
    }

    @Test
    void passwordChangeRequest_rejectsNewPasswordWithoutUppercase() {
        Set<ConstraintViolation<PasswordChangeRequest>> violations =
                validator.validate(validPasswordChangeRequest("newpass12"));
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"));
    }

    @Test
    void passwordChangeRequest_rejectsNewPasswordWithoutDigit() {
        Set<ConstraintViolation<PasswordChangeRequest>> violations =
                validator.validate(validPasswordChangeRequest("NewPassword"));
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"));
    }
}
