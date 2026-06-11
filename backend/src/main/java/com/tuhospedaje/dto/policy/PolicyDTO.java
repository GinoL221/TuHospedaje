package com.tuhospedaje.dto.policy;

import com.tuhospedaje.entity.Policy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PolicyDTO {
    @Null(message = "El id debe ser nulo al crear")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String description;

    @NotBlank(message = "El ícono es obligatorio")
    private String icon;

    public Policy toEntity() {
        Policy policy = new Policy();
        policy.setName(this.name);
        policy.setDescription(this.description);
        policy.setIcon(this.icon);
        return policy;
    }

    public static PolicyDTO fromEntity(Policy policy) {
        PolicyDTO dto = new PolicyDTO();
        dto.setId(policy.getId());
        dto.setName(policy.getName());
        dto.setDescription(policy.getDescription());
        dto.setIcon(policy.getIcon());
        return dto;
    }
}
