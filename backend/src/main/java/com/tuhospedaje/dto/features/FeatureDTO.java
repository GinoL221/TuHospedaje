package com.tuhospedaje.dto.features;

import com.tuhospedaje.entity.Feature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeatureDTO {

    @Null(message = "El id debe ser nulo al crear")
    private Long id;

    @NotBlank(message = "El nombre de la característica es obligatorio")
    private String name;

    @NotBlank(message = "La icono de la característica es obligatoria")
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
