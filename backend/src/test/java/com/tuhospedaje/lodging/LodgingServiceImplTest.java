package com.tuhospedaje.lodging;

import com.tuhospedaje.dto.LodgingDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.service.impl.LodgingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LodgingServiceImplTest {

    @Mock
    private LodgingRepository lodgingRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private LodgingServiceImpl lodgingService;

    @Test
    void shouldAssignCategoryOnSaveWhenCategoryIdExists() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Hotel");

        LodgingDTO dto = new LodgingDTO();
        dto.setName("Gran Hotel");
        dto.setAddress("Dirección");
        dto.setCity("Ciudad");
        dto.setCountry("País");
        dto.setPhoneNumber("123456");
        dto.setEmail("test@lodging.com");
        dto.setCategoryId(1L);

        Lodging savedEntity = new Lodging();
        savedEntity.setId(99L);
        savedEntity.setName("Gran Hotel");
        savedEntity.setAddress("Dirección");
        savedEntity.setCity("Ciudad");
        savedEntity.setCountry("País");
        savedEntity.setPhoneNumber("123456");
        savedEntity.setEmail("test@lodging.com");
        savedEntity.setCategory(category);

        when(lodgingRepository.existsByName("Gran Hotel")).thenReturn(false);
        when(lodgingRepository.existsByEmail("test@lodging.com")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(lodgingRepository.save(any(Lodging.class))).thenReturn(savedEntity);

        LodgingDTO response = lodgingService.save(dto);

        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("Hotel");
    }

    @Test
    void shouldThrowWhenSaveUsesMissingCategory() {
        LodgingDTO dto = new LodgingDTO();
        dto.setName("Gran Hotel");
        dto.setAddress("Dirección");
        dto.setCity("Ciudad");
        dto.setCountry("País");
        dto.setPhoneNumber("123456");
        dto.setEmail("test@lodging.com");
        dto.setCategoryId(999L);

        when(lodgingRepository.existsByName("Gran Hotel")).thenReturn(false);
        when(lodgingRepository.existsByEmail("test@lodging.com")).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> lodgingService.save(dto));

        assertThat(ex.getMessage()).isEqualTo("Categoría no encontrada");
    }

    @Test
    void shouldClearCategoryOnUpdateWhenCategoryIdIsNull() {
        Category oldCategory = new Category();
        oldCategory.setId(2L);
        oldCategory.setName("Hostel");

        Lodging existing = new Lodging();
        existing.setId(20L);
        existing.setName("Refugio");
        existing.setAddress("Dir");
        existing.setCity("City");
        existing.setCountry("Country");
        existing.setPhoneNumber("555");
        existing.setEmail("refugio@test.com");
        existing.setCategory(oldCategory);

        LodgingDTO dto = new LodgingDTO();
        dto.setId(20L);
        dto.setName("Refugio");
        dto.setAddress("Dir");
        dto.setCity("City");
        dto.setCountry("Country");
        dto.setPhoneNumber("555");
        dto.setEmail("refugio@test.com");
        dto.setCategoryId(null);

        when(lodgingRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(lodgingRepository.save(any(Lodging.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LodgingDTO response = lodgingService.update(dto);

        assertThat(response.getCategoryId()).isNull();
        assertThat(response.getCategoryName()).isNull();
    }

    @Test
    void shouldAssignCategoryOnUpdateWhenCategoryIdExists() {
        Category category = new Category();
        category.setId(3L);
        category.setName("Cabaña");

        Lodging existing = new Lodging();
        existing.setId(21L);
        existing.setName("Refugio");
        existing.setAddress("Dir");
        existing.setCity("City");
        existing.setCountry("Country");
        existing.setPhoneNumber("555");
        existing.setEmail("refugio@test.com");

        LodgingDTO dto = new LodgingDTO();
        dto.setId(21L);
        dto.setName("Refugio");
        dto.setAddress("Dir");
        dto.setCity("City");
        dto.setCountry("Country");
        dto.setPhoneNumber("555");
        dto.setEmail("refugio@test.com");
        dto.setCategoryId(3L);

        when(lodgingRepository.findById(21L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(lodgingRepository.save(any(Lodging.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LodgingDTO response = lodgingService.update(dto);

        assertThat(response.getCategoryId()).isEqualTo(3L);
        assertThat(response.getCategoryName()).isEqualTo("Cabaña");
    }
}
