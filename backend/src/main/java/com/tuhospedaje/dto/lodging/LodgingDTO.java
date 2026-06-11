package com.tuhospedaje.dto.lodging;

import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.LodgingImage;
import com.tuhospedaje.entity.Policy;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class LodgingDTO {
    @Null(message = "El id debe ser nulo al crear")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String description;

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    @NotBlank(message = "La ciudad es obligatoria")
    private String city;

    @NotBlank(message = "El país es obligatorio")
    private String country;

    @NotBlank(message = "El número de teléfono es obligatorio")
    private String phoneNumber;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    private List<String> imageUrls;
    private Long categoryId;
    private String categoryName;
    private Set<Long> featureIds;
    private List<Map<String, Object>> features;

    @Positive(message = "El precio por noche debe ser positivo")
    private BigDecimal pricePerNight;

    @Positive(message = "La cantidad máxima de huéspedes debe ser positiva")
    private Integer maxGuests;

    private Set<Long> policyIds;
    private List<Map<String, Object>> policies;
    private Double averageRating;
    private Integer ratingCount;

    public Lodging toEntity() {
        Lodging lodging = new Lodging();
        lodging.setName(this.name);
        lodging.setDescription(this.description);
        lodging.setAddress(this.address);
        lodging.setCity(this.city);
        lodging.setCountry(this.country);
        lodging.setPhoneNumber(this.phoneNumber);
        lodging.setEmail(this.email);
        lodging.setPricePerNight(this.pricePerNight);
        lodging.setMaxGuests(this.maxGuests);
        return lodging;
    }


    public static LodgingDTO fromEntity(Lodging lodging) {
        LodgingDTO dto = new LodgingDTO();
        dto.setId(lodging.getId());
        dto.setName(lodging.getName());
        dto.setDescription(lodging.getDescription());
        dto.setAddress(lodging.getAddress());
        dto.setCity(lodging.getCity());
        dto.setCountry(lodging.getCountry());
        dto.setPhoneNumber(lodging.getPhoneNumber());
        dto.setEmail(lodging.getEmail());
        dto.setPricePerNight(lodging.getPricePerNight());
        dto.setMaxGuests(lodging.getMaxGuests());
        if (lodging.getCategory() != null) {
            dto.setCategoryId(lodging.getCategory().getId());
            dto.setCategoryName(lodging.getCategory().getName());
        }
        if (lodging.getFeatures() != null) {
            dto.setFeatureIds(lodging.getFeatures().stream().map(Feature::getId).collect(Collectors.toSet()));
            dto.setFeatures(lodging.getFeatures().stream().map(f -> {
                Map<String, Object> feat = new java.util.HashMap<>();
                feat.put("id", f.getId());
                feat.put("name", f.getName());
                feat.put("icon", f.getIcon());
                return feat;
            }).collect(Collectors.toList()));
        }

        if (lodging.getPolicies() != null) {
            dto.setPolicyIds(lodging.getPolicies().stream().map(Policy::getId).collect(Collectors.toSet()));
            dto.setPolicies(lodging.getPolicies().stream().map(p -> {
                Map<String, Object> pol = new java.util.HashMap<>();
                pol.put("id", p.getId());
                pol.put("name", p.getName());
                pol.put("description", p.getDescription());
                pol.put("icon", p.getIcon());
                return pol;
            }).collect(Collectors.toList()));
        }

        if (lodging.getImages() != null) {
            dto.setImageUrls(lodging.getImages().stream()
                    .map(LodgingImage::getImageUrl)
                    .toList());
        }
        return dto;
    }
}
