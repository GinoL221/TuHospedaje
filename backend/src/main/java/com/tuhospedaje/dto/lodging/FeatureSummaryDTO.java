package com.tuhospedaje.dto.lodging;

import com.tuhospedaje.entity.Feature;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Read-only summary of an amenity feature embedded in a lodging response")
public class FeatureSummaryDTO {

    @Schema(description = "Unique identifier of the feature", example = "7")
    private Long id;

    @Schema(description = "Name of the amenity feature", example = "Swimming Pool")
    private String name;

    @Schema(description = "Icon identifier or URL representing the feature", example = "fa-swimming-pool")
    private String icon;

    public static FeatureSummaryDTO fromEntity(Feature feature) {
        FeatureSummaryDTO dto = new FeatureSummaryDTO();
        dto.setId(feature.getId());
        dto.setName(feature.getName());
        dto.setIcon(feature.getIcon());
        return dto;
    }
}
