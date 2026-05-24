package com.tuhospedaje.lodging;

import com.tuhospedaje.dto.LodgingDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LodgingDTOTest {

    @Mock
    private CategoryRepository categoryRepository;

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
}
