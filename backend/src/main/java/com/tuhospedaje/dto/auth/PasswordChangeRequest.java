package com.tuhospedaje.dto.auth;

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
@Schema(description = "Request body to change the authenticated user's password")
public class PasswordChangeRequest {

    @NotBlank(message = "{error.auth.password.required}")
    @Size(min = 6, max = 20, message = "{error.auth.password.size}")
    @Schema(description = "The caller's current password, verified before the change is applied", minLength = 6, maxLength = 20)
    private String currentPassword;

    @NotBlank(message = "{error.auth.password.required}")
    @Size(min = 6, max = 20, message = "{error.auth.password.size}")
    @Pattern(regexp = ".*[A-Z].*", message = "{error.auth.password.uppercase}")
    @Pattern(regexp = ".*[0-9].*", message = "{error.auth.password.number}")
    @Schema(description = "The new password to set (6–20 characters, at least 1 uppercase, 1 digit)", example = "s3cureP@ss", minLength = 6, maxLength = 20)
    private String newPassword;
}
