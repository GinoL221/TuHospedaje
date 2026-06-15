package com.tuhospedaje.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Request body to update the role assigned to a user")
public class RoleRequest {

    @NotBlank(message = "El rol es obligatorio")
    @Schema(description = "Target role name to assign to the user (e.g. ADMIN, USER)", example = "ADMIN")
    private String role;
}
