package com.tuhospedaje.lodging;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.LodgingImage;
import com.tuhospedaje.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LodgingDTOTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void shouldMapToEntityWithoutCategory() {
        LodgingDTO dto = new LodgingDTO();
        dto.setName("Gran Hotel");
        dto.setDescription("Descripción");
        dto.setAddress("Calle 123");
        dto.setCity("Ciudad");
        dto.setCountry("País");
        dto.setPhoneNumber("123456");
        dto.setEmail("hotel@test.com");

        Lodging entity = dto.toEntity();

        assertThat(entity.getName()).isEqualTo("Gran Hotel");
        assertThat(entity.getDescription()).isEqualTo("Descripción");
        assertThat(entity.getAddress()).isEqualTo("Calle 123");
        assertThat(entity.getCity()).isEqualTo("Ciudad");
        assertThat(entity.getCountry()).isEqualTo("País");
        assertThat(entity.getPhoneNumber()).isEqualTo("123456");
        assertThat(entity.getEmail()).isEqualTo("hotel@test.com");
        assertThat(entity.getCategory()).isNull();
    }

    @Test
    void shouldMapCategoryInToEntityWhenCategoryIdProvided() {
        Category category = new Category();
        category.setId(10L);
        category.setName("Hotel");

        LodgingDTO dto = new LodgingDTO();
        dto.setName("Gran Hotel");
        dto.setCategoryId(10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        Lodging entity = dto.toEntity(categoryRepository);

        assertThat(entity.getCategory()).isNotNull();
        assertThat(entity.getCategory().getId()).isEqualTo(10L);
    }

    @Test
    void shouldThrowWhenCategoryIdInToEntityDoesNotExist() {
        LodgingDTO dto = new LodgingDTO();
        dto.setCategoryId(999L);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> dto.toEntity(categoryRepository));
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
