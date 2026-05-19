package com.tuhospedaje.dto;

import com.tuhospedaje.entity.Lodging;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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

    public Lodging toEntity() {
        Lodging lodging = new Lodging();
        lodging.setName(this.name);
        lodging.setDescription(this.description);
        lodging.setAddress(this.address);
        lodging.setCity(this.city);
        lodging.setCountry(this.country);
        lodging.setPhoneNumber(this.phoneNumber);
        lodging.setEmail(this.email);
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
        if (lodging.getImages() != null) {
            dto.setImageUrls(lodging.getImages().stream()
                    .map(img -> img.getImageUrl())
                    .toList());
        }
        return dto;
    }
}
