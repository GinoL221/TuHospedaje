package com.tuhospedaje.dto.features;

import com.tuhospedaje.entity.Feature;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Amenity feature data transfer object")
public class FeatureDTO {

    @Null(message = "El id debe ser nulo al crear")
    @Schema(description = "Unique identifier of the feature (null on create)", example = "7")
    private Long id;

    @NotBlank(message = "El nombre de la característica es obligatorio")
    @Schema(description = "Name of the amenity feature", example = "Swimming Pool")
    private String name;

    @NotBlank(message = "La icono de la característica es obligatoria")
    @Schema(description = "Icon identifier or URL representing the feature", example = "fa-swimming-pool")
    private String icon;

    public Feature toEntity() {
        Feature feature = new Feature();
        feature.setName(this.name);
        feature.setIcon(this.icon);
        return feature;
    }

    public static FeatureDTO fromEntity(Feature feature) {
        FeatureDTO dto = new FeatureDTO();
        dto.setId(feature.getId());
        dto.setName(feature.getName());
        dto.setIcon(feature.getIcon());
        return dto;
    }
}
