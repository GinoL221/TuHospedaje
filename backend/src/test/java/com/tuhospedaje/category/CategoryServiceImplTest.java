package com.tuhospedaje.category;

import com.tuhospedaje.dto.CategoryDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LodgingRepository lodgingRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void shouldCreateCategorySuccessfully() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Hotel");
        dto.setDescription("Alojamiento urbano");

        Category savedEntity = new Category();
        savedEntity.setId(1L);
        savedEntity.setName("Hotel");
        savedEntity.setDescription("Alojamiento urbano");

        when(categoryRepository.existsByName("Hotel")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedEntity);

        CategoryDTO response = categoryService.save(dto);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Hotel");
        assertThat(response.getDescription()).isEqualTo("Alojamiento urbano");
    }

    @Test
    void shouldThrowWhenCreateCategoryNameAlreadyExists() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Cabaña");
        dto.setDescription("Primera");

        when(categoryRepository.existsByName("Cabaña")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> categoryService.save(dto));
    }

    @Test
    void shouldReturnAllCategories() {
        Category categoryOne = new Category();
        categoryOne.setId(1L);
        categoryOne.setName("Hotel");

        Category categoryTwo = new Category();
        categoryTwo.setId(2L);
        categoryTwo.setName("Hostel");

        when(categoryRepository.findAll()).thenReturn(List.of(categoryOne, categoryTwo));

        List<CategoryDTO> response = categoryService.findAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("Hostel");
        assertThat(response.get(1).getName()).isEqualTo("Hotel");
    }

    @Test
    void shouldReturnCategoryById() {
        Category category = new Category();
        category.setId(9L);
        category.setName("Posada");

        when(categoryRepository.findById(9L)).thenReturn(Optional.of(category));

        Optional<CategoryDTO> response = categoryService.findById(9L);

        assertThat(response).isPresent();
        assertThat(response.get().getId()).isEqualTo(9L);
        assertThat(response.get().getName()).isEqualTo("Posada");
    }

    @Test
    void shouldReturnEmptyWhenCategoryByIdDoesNotExist() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<CategoryDTO> response = categoryService.findById(999L);

        assertThat(response).isEmpty();
    }

    @Test
    void shouldUpdateCategorySuccessfully() {
        Category existing = new Category();
        existing.setId(1L);
        existing.setName("Hotel");
        existing.setDescription("Original");

        Category updated = new Category();
        updated.setId(1L);
        updated.setName("Hotel Boutique");
        updated.setDescription("Actualizado");

        CategoryDTO input = new CategoryDTO();
        input.setId(1L);
        input.setName("Hotel Boutique");
        input.setDescription("Actualizado");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByNameIgnoreCase("Hotel Boutique")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(updated);

        CategoryDTO response = categoryService.update(input);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Hotel Boutique");
        assertThat(response.getDescription()).isEqualTo("Actualizado");
    }

    @Test
    void shouldThrowWhenUpdateCategoryDoesNotExist() {
        CategoryDTO input = new CategoryDTO();
        input.setId(777L);
        input.setName("No existe");

        when(categoryRepository.findById(777L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.update(input));
    }

    @Test
    void shouldDeleteCategorySuccessfully() {
        Category category = new Category();
        category.setId(8L);
        category.setName("Temporal");

        when(categoryRepository.findById(8L)).thenReturn(Optional.of(category));
        when(lodgingRepository.countByCategoryId(8L)).thenReturn(0L);

        Optional<CategoryDTO> deleted = categoryService.delete(8L);

        assertThat(deleted).isPresent();
        assertThat(deleted.get().getId()).isEqualTo(8L);
    }

    @Test
    void shouldThrowWhenDeleteCategoryWithLodgings() {
        Category category = new Category();
        category.setId(5L);
        category.setName("Hotel");

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(lodgingRepository.countByCategoryId(5L)).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> categoryService.delete(5L));
    }

    @Test
    void shouldThrowWhenDeleteCategoryDoesNotExist() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.delete(404L));
    }
}
