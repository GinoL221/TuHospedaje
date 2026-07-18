package com.tuhospedaje.dto.lodging;

import com.tuhospedaje.entity.Policy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Read-only summary of a lodging policy embedded in a lodging response")
public class PolicySummaryDTO {

    @Schema(description = "Unique identifier of the policy", example = "2")
    private Long id;

    @Schema(description = "Name of the policy", example = "No Smoking")
    private String name;

    @Schema(description = "Detailed description of the policy", example = "Smoking is strictly prohibited in all indoor areas and common spaces.")
    private String description;

    @Schema(description = "Icon identifier or URL representing the policy", example = "fa-ban-smoking")
    private String icon;

    public static PolicySummaryDTO fromEntity(Policy policy) {
        PolicySummaryDTO dto = new PolicySummaryDTO();
        dto.setId(policy.getId());
        dto.setName(policy.getName());
        dto.setDescription(policy.getDescription());
        dto.setIcon(policy.getIcon());
        return dto;
    }
}
