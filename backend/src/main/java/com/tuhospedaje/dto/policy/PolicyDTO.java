package com.tuhospedaje.dto.policy;

import com.tuhospedaje.entity.Policy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Lodging policy (house rule) data transfer object")
public class PolicyDTO {

    @Null(message = "El id debe ser nulo al crear")
    @Schema(description = "Unique identifier of the policy (null on create)", example = "2")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Name of the policy", example = "No Smoking")
    private String name;

    @Schema(description = "Detailed description of the policy", example = "Smoking is strictly prohibited in all indoor areas and common spaces.")
    private String description;

    @NotBlank(message = "El ícono es obligatorio")
    @Schema(description = "Icon identifier or URL representing the policy", example = "fa-ban-smoking")
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
