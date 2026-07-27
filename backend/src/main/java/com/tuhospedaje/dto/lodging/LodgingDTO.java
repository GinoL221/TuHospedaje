package com.tuhospedaje.dto.lodging;

import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.LodgingImage;
import com.tuhospedaje.entity.Policy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Schema(description = "Lodging listing data transfer object")
public class LodgingDTO {

    @Null(message = "El id debe ser nulo al crear")
    @Schema(description = "Unique identifier of the lodging (null on create)", example = "42")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Name of the lodging", example = "Hotel Patagonia Sur")
    private String name;

    @Schema(description = "Detailed description of the lodging", example = "A cozy boutique hotel in the heart of Bariloche with mountain views.")
    private String description;

    @NotBlank(message = "La dirección es obligatoria")
    @Schema(description = "Street address of the lodging", example = "Av. San Martín 456")
    private String address;

    @NotBlank(message = "La ciudad es obligatoria")
    @Schema(description = "City where the lodging is located", example = "Bariloche")
    private String city;

    @NotBlank(message = "El país es obligatorio")
    @Schema(description = "Country where the lodging is located", example = "Argentina")
    private String country;

    @NotBlank(message = "El número de teléfono es obligatorio")
    @Schema(description = "Contact phone number", example = "+54 294 442-1100")
    private String phoneNumber;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Schema(description = "Contact email address", example = "info@hotelpatagoniaur.com")
    private String email;

    @Schema(description = "List of image URLs for the lodging", example = "[\"https://cdn.example.com/lodgings/42/main.jpg\", \"https://cdn.example.com/lodgings/42/room1.jpg\"]")
    private List<String> imageUrls;

    @Schema(description = "ID of the category this lodging belongs to", example = "3")
    private Long categoryId;

    @Schema(description = "Name of the category this lodging belongs to", example = "Cabin")
    private String categoryName;

    @Schema(description = "Set of feature IDs associated with this lodging", example = "[1, 5, 7]")
    private Set<Long> featureIds;

    @Schema(description = "Full feature objects (id, name, icon) associated with this lodging")
    private List<FeatureSummaryDTO> features;

    @NotNull(message = "El precio por noche es obligatorio")
    @Positive(message = "El precio por noche debe ser positivo")
    @Schema(description = "Price per night in ARS", example = "120000.00")
    private BigDecimal pricePerNight;

    @NotNull(message = "La cantidad máxima de huéspedes es obligatoria")
    @Positive(message = "La cantidad máxima de huéspedes debe ser positiva")
    @Schema(description = "Maximum number of guests allowed", example = "4")
    private Integer maxGuests;

    @Schema(description = "Set of policy IDs associated with this lodging", example = "[2, 6]")
    private Set<Long> policyIds;

    @Schema(description = "Full policy objects (id, name, description, icon) associated with this lodging")
    private List<PolicySummaryDTO> policies;

    @Schema(description = "Average guest rating (0.0 – 5.0)", example = "4.7")
    private Double averageRating;

    @Schema(description = "Total number of ratings received", example = "238")
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
            dto.setFeatures(lodging.getFeatures().stream()
                    .map(FeatureSummaryDTO::fromEntity)
                    .collect(Collectors.toList()));
        }

        if (lodging.getPolicies() != null) {
            dto.setPolicyIds(lodging.getPolicies().stream().map(Policy::getId).collect(Collectors.toSet()));
            dto.setPolicies(lodging.getPolicies().stream()
                    .map(PolicySummaryDTO::fromEntity)
                    .collect(Collectors.toList()));
        }

        if (lodging.getImages() != null) {
            dto.setImageUrls(lodging.getImages().stream()
                    .map(LodgingImage::getImageUrl)
                    .toList());
        }
        return dto;
    }
}
