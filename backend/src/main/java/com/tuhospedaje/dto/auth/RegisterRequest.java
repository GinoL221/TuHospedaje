package com.tuhospedaje.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data required to register a new user account")
public class RegisterRequest {

    @NotBlank
    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @NotBlank
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @NotBlank
    @Email
    @Schema(description = "Valid email address — used as login identifier", example = "john.doe@example.com")
    private String email;

    @NotBlank(message = "{error.auth.password.required}")
    @Size(min = 6, max = 20, message = "{error.auth.password.size}")
    @Pattern(regexp = ".*[A-Z].*", message = "{error.auth.password.uppercase}")
    @Pattern(regexp = ".*[0-9].*", message = "{error.auth.password.number}")
    @Schema(description = "Account password (6–20 characters, at least 1 uppercase, 1 digit)", example = "s3cureP@ss", minLength = 6, maxLength = 20)
    private String password;
}
