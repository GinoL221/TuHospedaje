package com.tuhospedaje.lodging;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.LodgingImage;
import com.tuhospedaje.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LodgingDTOTest {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    private LodgingDTO createValidLodgingDTO() {
        LodgingDTO dto = new LodgingDTO();
        dto.setName("Gran Hotel");
        dto.setDescription("Descripción");
        dto.setAddress("Calle 123");
        dto.setCity("Ciudad");
        dto.setCountry("País");
        dto.setPhoneNumber("123456");
        dto.setEmail("hotel@test.com");
        dto.setPricePerNight(new BigDecimal("150.00"));
        dto.setMaxGuests(4);
        return dto;
    }

    @Test
    void shouldPassValidationWhenDtoIsValid() {
        LodgingDTO dto = createValidLodgingDTO();
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidationWhenNameIsBlank() {
        LodgingDTO dto = createValidLodgingDTO();
        dto.setName("");
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldFailValidationWhenAddressIsBlank() {
        LodgingDTO dto = createValidLodgingDTO();
        dto.setAddress("   ");
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldFailValidationWhenCityIsBlank() {
        LodgingDTO dto = createValidLodgingDTO();
        dto.setCity(null);
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldFailValidationWhenCountryIsBlank() {
        LodgingDTO dto = createValidLodgingDTO();
        dto.setCountry("");
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldFailValidationWhenPhoneNumberIsBlank() {
        LodgingDTO dto = createValidLodgingDTO();
        dto.setPhoneNumber("");
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldFailValidationWhenEmailIsBlank() {
        LodgingDTO dto = createValidLodgingDTO();
        dto.setEmail("");
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldFailValidationWhenEmailIsInvalid() {
        LodgingDTO dto = createValidLodgingDTO();
        dto.setEmail("not-an-email");
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldFailValidationWhenPricePerNightIsNegativeOrZero() {
        LodgingDTO dto = createValidLodgingDTO();
        dto.setPricePerNight(BigDecimal.ZERO);
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();

        dto.setPricePerNight(new BigDecimal("-1.00"));
        violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldFailValidationWhenMaxGuestsIsNegativeOrZero() {
        LodgingDTO dto = createValidLodgingDTO();
        dto.setMaxGuests(0);
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();

        dto.setMaxGuests(-5);
        violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldFailValidationWhenIdIsNotNull() {
        LodgingDTO dto = createValidLodgingDTO();
        dto.setId(1L);
        Set<ConstraintViolation<LodgingDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldMapCategoryFieldsFromEntityWhenCategoryPresent() {
        Category category = new Category();
        category.setId(5L);
        category.setName("Cabaña");

        Lodging lodging = new Lodging();
        lodging.setId(2L);
        lodging.setName("Refugio");
        lodging.setCategory(category);

        LodgingDTO dto = LodgingDTO.fromEntity(lodging);

        assertThat(dto.getCategoryId()).isEqualTo(5L);
        assertThat(dto.getCategoryName()).isEqualTo("Cabaña");
    }

    @Test
    void shouldMapNullCategoryFieldsFromEntityWhenCategoryMissing() {
        Lodging lodging = new Lodging();
        lodging.setId(3L);
        lodging.setName("Sin Categoría");

        LodgingDTO dto = LodgingDTO.fromEntity(lodging);

        assertThat(dto.getCategoryId()).isNull();
        assertThat(dto.getCategoryName()).isNull();
    }

    @Test
    void shouldMapImageUrlsFromEntity() {
        LodgingImage img1 = new LodgingImage();
        img1.setImageUrl("https://picsum.photos/1");
        LodgingImage img2 = new LodgingImage();
        img2.setImageUrl("https://picsum.photos/2");

        Lodging lodging = new Lodging();
        lodging.setId(4L);
        lodging.setName("Con Imágenes");
        lodging.setImages(List.of(img1, img2));

        LodgingDTO dto = LodgingDTO.fromEntity(lodging);

        assertThat(dto.getImageUrls()).hasSize(2);
        assertThat(dto.getImageUrls()).containsExactly("https://picsum.photos/1", "https://picsum.photos/2");
    }

    @Test
    void shouldMapEmptyImageUrlsWhenEntityHasNoImages() {
        Lodging lodging = new Lodging();
        lodging.setId(5L);
        lodging.setName("Sin Imágenes");
        lodging.setImages(null);

        LodgingDTO dto = LodgingDTO.fromEntity(lodging);

        assertThat(dto.getImageUrls()).isNull();
    }

    @Test
    void shouldMapPriceAndGuestsInToEntity() {
        LodgingDTO dto = new LodgingDTO();
        dto.setName("Hotel Precio");
        dto.setPricePerNight(new BigDecimal("150.00"));
        dto.setMaxGuests(4);

        Lodging entity = dto.toEntity();

        assertThat(entity.getPricePerNight()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(entity.getMaxGuests()).isEqualTo(4);
    }

    @Test
    void shouldMapPriceAndGuestsFromEntity() {
        Lodging lodging = new Lodging();
        lodging.setPricePerNight(new BigDecimal("200.00"));
        lodging.setMaxGuests(6);

        LodgingDTO dto = LodgingDTO.fromEntity(lodging);

        assertThat(dto.getPricePerNight()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(dto.getMaxGuests()).isEqualTo(6);
    }

    @Test
    void shouldMapAllBasicFieldsFromEntity() {
        Lodging lodging = new Lodging();
        lodging.setId(10L);
        lodging.setName("Hotel Completo");
        lodging.setDescription("Buena vista");
        lodging.setAddress("Av. Principal 555");
        lodging.setCity("Buenos Aires");
        lodging.setCountry("Argentina");
        lodging.setPhoneNumber("+54111234567");
        lodging.setEmail("completo@test.com");

        LodgingDTO dto = LodgingDTO.fromEntity(lodging);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo("Hotel Completo");
        assertThat(dto.getDescription()).isEqualTo("Buena vista");
        assertThat(dto.getAddress()).isEqualTo("Av. Principal 555");
        assertThat(dto.getCity()).isEqualTo("Buenos Aires");
        assertThat(dto.getCountry()).isEqualTo("Argentina");
        assertThat(dto.getPhoneNumber()).isEqualTo("+54111234567");
        assertThat(dto.getEmail()).isEqualTo("completo@test.com");
    }
}
