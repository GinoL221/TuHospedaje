package com.tuhospedaje.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleRequest {
    @NotBlank(message = "El rol es obligatorio")
    private String role;
}
