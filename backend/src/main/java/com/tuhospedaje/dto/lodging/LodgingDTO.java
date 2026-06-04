package com.tuhospedaje.dto.lodging;

import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.LodgingImage;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.CategoryRepository;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class LodgingDTO {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String country;
    private String phoneNumber;
    private String email;
    private List<String> imageUrls;
    private Long categoryId;
    private String categoryName;
    private Set<Long> featureIds;
    private List<Map<String, Object>> features;
    private BigDecimal pricePerNight;
    private Integer maxGuests;
    private Set<Long> policyIds;
    private List<Map<String, Object>> policies;

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

    public Lodging toEntity(CategoryRepository categoryRepository) {
        Lodging lodging = this.toEntity();

        if (this.categoryId != null) {
            Category category = categoryRepository.findById(this.categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
            lodging.setCategory(category);
        }

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
