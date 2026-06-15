package com.tuhospedaje.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credentials required to authenticate a user")
public class LoginRequest {

    @NotBlank
    @Schema(description = "Registered email address of the user", example = "john.doe@example.com")
    private String email;

    @NotBlank
    @Schema(description = "Account password (min 6 characters)", example = "s3cureP@ss")
    private String password;
}
