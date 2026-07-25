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
@Schema(description = "Request body to enable or disable a user account")
public class UserStatusRequest {

    @Schema(description = "Whether the account should be enabled", example = "false")
    private boolean enabled;
}
