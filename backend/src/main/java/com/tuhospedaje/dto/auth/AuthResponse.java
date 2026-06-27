package com.tuhospedaje.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authenticated user's claims. The JWT itself never appears here — "
        + "it is delivered exclusively via the httpOnly ACCESS_TOKEN cookie.")
public class AuthResponse {

    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "User's role", example = "USER")
    private String role;

    @Schema(description = "User's avatar/profile image URL")
    private String imageUrl;
}
