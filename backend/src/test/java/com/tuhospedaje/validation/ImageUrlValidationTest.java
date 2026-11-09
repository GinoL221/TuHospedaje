package com.tuhospedaje.validation;

import com.tuhospedaje.dto.category.CategoryDTO;
import com.tuhospedaje.dto.category.CategoryValidationGroups;
import com.tuhospedaje.dto.lodging.LodgingDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUrlValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void categoryAcceptsLocalUploadPath() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Hoteles");
        dto.setDescription("Descripción");
        dto.setImageUrl("/uploads/3f2c9a1e-8c1d-4b7e-9f0a-2d6e5b8c7a1f.jpg");

        Set<ConstraintViolation<CategoryDTO>> violations =
                validator.validate(dto, CategoryValidationGroups.OnCreate.class);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("imageUrl"));
    }

    @Test
    void categoryAcceptsCanonicalAssetPath() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Hoteles");
        dto.setDescription("Descripción");
        dto.setImageUrl("/canonical-lodging-images/hotel.jpg");

        Set<ConstraintViolation<CategoryDTO>> violations =
                validator.validate(dto, CategoryValidationGroups.OnCreate.class);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("imageUrl"));
    }

    @Test
    void categoryAcceptsAbsoluteHttpsUrl() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Hoteles");
        dto.setDescription("Descripción");
        dto.setImageUrl("https://cdn.example.com/categories/hotel.jpg");

        Set<ConstraintViolation<CategoryDTO>> violations =
                validator.validate(dto, CategoryValidationGroups.OnCreate.class);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("imageUrl"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://insecure.example.com/img.jpg",
            "javascript:alert(1)",
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==",
            "file:///etc/passwd",
            "/uploads/../../etc/passwd",
            "/uploads/secret.txt..",
            "",
            "   "
    })
    void categoryRejectsInsecureOrMalformedImageUrls(String invalidUrl) {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Hoteles");
        dto.setDescription("Descripción");
        dto.setImageUrl(invalidUrl);

        Set<ConstraintViolation<CategoryDTO>> violations =
                validator.validate(dto, CategoryValidationGroups.OnUpdate.class);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("imageUrl"));
    }

    @Test
    void lodgingRejectsInsecureOrMalformedImageUrls() {
        LodgingDTO dto = validLodgingDTO();
        dto.setImageUrls(List.of("https://example.com/good.jpg", "javascript:alert(1)"));

        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("imageUrls"));
    }

    @Test
    void lodgingAcceptsHttpsAndLocalUploadUrls() {
        LodgingDTO dto = validLodgingDTO();
        dto.setImageUrls(List.of(
                "https://cdn.example.com/lodging.jpg",
                "/uploads/3f2c9a1e-8c1d-4b7e-9f0a-2d6e5b8c7a1f.jpg",
                "/canonical-lodging-images/lodging1.jpg"
        ));

        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().contains("imageUrls"));
    }

    @Test
    void acceptsHttpsImageUrlOfExactly255Characters() {
        String base = "https://cdn.example.com/";
        String suffix = ".jpg";
        String filler = "a".repeat(255 - base.length() - suffix.length());
        String url255 = base + filler + suffix;
        assertThat(url255).hasSize(255);

        CategoryDTO dto = new CategoryDTO();
        dto.setName("Hoteles");
        dto.setDescription("Descripción");
        dto.setImageUrl(url255);

        Set<ConstraintViolation<CategoryDTO>> violations =
                validator.validate(dto, CategoryValidationGroups.OnCreate.class);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("imageUrl"));
    }

    @Test
    void rejectsHttpsImageUrlExceeding255Characters() {
        String base = "https://cdn.example.com/";
        String suffix = ".jpg";
        String filler = "a".repeat(256 - base.length() - suffix.length());
        String url256 = base + filler + suffix;
        assertThat(url256).hasSize(256);

        CategoryDTO dto = new CategoryDTO();
        dto.setName("Hoteles");
        dto.setDescription("Descripción");
        dto.setImageUrl(url256);

        Set<ConstraintViolation<CategoryDTO>> violations =
                validator.validate(dto, CategoryValidationGroups.OnCreate.class);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("imageUrl"));

        LodgingDTO lodging = validLodgingDTO();
        lodging.setImageUrls(List.of(url256));
        Set<ConstraintViolation<LodgingDTO>> lodgingViolations = validator.validate(lodging);
        assertThat(lodgingViolations).anyMatch(v -> v.getPropertyPath().toString().contains("imageUrls"));
    }

    private static LodgingDTO validLodgingDTO() {
        LodgingDTO dto = new LodgingDTO();
        dto.setName("Hotel Valido");
        dto.setDescription("Desc");
        dto.setAddress("Calle 123");
        dto.setCity("Bariloche");
        dto.setCountry("Argentina");
        dto.setPhoneNumber("+54000");
        dto.setEmail("hotel@test.com");
        dto.setPricePerNight(new BigDecimal("100.00"));
        dto.setMaxGuests(2);
        return dto;
    }
}
