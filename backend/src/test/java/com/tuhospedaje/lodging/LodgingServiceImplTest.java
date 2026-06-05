package com.tuhospedaje.lodging;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.service.impl.LodgingServiceImpl;
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
class LodgingServiceImplTest {

    @Mock
    private LodgingRepository lodgingRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private LodgingServiceImpl lodgingService;

    @Test
    void shouldSaveLodgingSuccessfully() {
        LodgingDTO dto = new LodgingDTO();
        dto.setName("Gran Hotel");
        dto.setAddress("Calle 123");
        dto.setCity("Ciudad");
        dto.setCountry("País");
        dto.setPhoneNumber("123456");
        dto.setEmail("hotel@test.com");

        Lodging savedEntity = new Lodging();
        savedEntity.setId(1L);
        savedEntity.setName("Gran Hotel");

        when(lodgingRepository.existsByName("Gran Hotel")).thenReturn(false);
        when(lodgingRepository.existsByEmail("hotel@test.com")).thenReturn(false);
        when(lodgingRepository.save(any(Lodging.class))).thenReturn(savedEntity);

        LodgingDTO response = lodgingService.save(dto);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Gran Hotel");
    }

    @Test
    void shouldThrowWhenSaveDuplicateName() {
        LodgingDTO dto = new LodgingDTO();
        dto.setName("Gran Hotel");
        dto.setEmail("hotel@test.com");

        when(lodgingRepository.existsByName("Gran Hotel")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> lodgingService.save(dto));
    }

    @Test
    void shouldThrowWhenSaveDuplicateEmail() {
        LodgingDTO dto = new LodgingDTO();
        dto.setName("Gran Hotel");
        dto.setEmail("dup@test.com");

        when(lodgingRepository.existsByName("Gran Hotel")).thenReturn(false);
        when(lodgingRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> lodgingService.save(dto));
    }

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
    void shouldReturnAllLodgings() {
        Lodging one = new Lodging();
        one.setId(1L);
        one.setName("Hotel A");

        Lodging two = new Lodging();
        two.setId(2L);
        two.setName("Hotel B");

        when(lodgingRepository.findAll()).thenReturn(List.of(one, two));

        List<LodgingDTO> response = lodgingService.findAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("Hotel A");
        assertThat(response.get(1).getName()).isEqualTo("Hotel B");
    }

    @Test
    void shouldReturnLodgingById() {
        Lodging lodging = new Lodging();
        lodging.setId(5L);
        lodging.setName("Hotel Central");

        when(lodgingRepository.findById(5L)).thenReturn(Optional.of(lodging));

        Optional<LodgingDTO> response = lodgingService.findById(5L);

        assertThat(response).isPresent();
        assertThat(response.get().getId()).isEqualTo(5L);
        assertThat(response.get().getName()).isEqualTo("Hotel Central");
    }

    @Test
    void shouldReturnEmptyWhenLodgingByIdDoesNotExist() {
        when(lodgingRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<LodgingDTO> response = lodgingService.findById(999L);

        assertThat(response).isEmpty();
    }

    @Test
    void shouldFindLodgingsByName() {
        Lodging lodging = new Lodging();
        lodging.setId(1L);
        lodging.setName("Hotel Boutique");

        when(lodgingRepository.findByNameContainingIgnoreCase("Boutique")).thenReturn(List.of(lodging));

        List<LodgingDTO> response = lodgingService.findByName("Boutique");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("Hotel Boutique");
    }

    @Test
    void shouldDeleteLodgingSuccessfully() {
        Lodging lodging = new Lodging();
        lodging.setId(8L);
        lodging.setName("Temporal");

        when(lodgingRepository.findById(8L)).thenReturn(Optional.of(lodging));

        Optional<LodgingDTO> deleted = lodgingService.delete(8L);

        assertThat(deleted).isPresent();
        assertThat(deleted.get().getId()).isEqualTo(8L);
    }

    @Test
    void shouldThrowWhenDeleteLodgingDoesNotExist() {
        when(lodgingRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> lodgingService.delete(404L));
    }

    @Test
    void shouldUpdateLodgingSuccessfully() {
        Lodging existing = new Lodging();
        existing.setId(1L);
        existing.setName("Hotel");
        existing.setEmail("hotel@test.com");

        LodgingDTO input = new LodgingDTO();
        input.setId(1L);
        input.setName("Hotel Boutique");
        input.setAddress("Av. Nueva 456");
        input.setCity("Ciudad");
        input.setCountry("País");
        input.setPhoneNumber("999");
        input.setEmail("hotel@test.com");

        when(lodgingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(lodgingRepository.save(any(Lodging.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LodgingDTO response = lodgingService.update(input);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Hotel Boutique");
        assertThat(response.getAddress()).isEqualTo("Av. Nueva 456");
    }

    @Test
    void shouldThrowWhenUpdateLodgingDoesNotExist() {
        LodgingDTO input = new LodgingDTO();
        input.setId(777L);
        input.setName("No existe");

        when(lodgingRepository.findById(777L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> lodgingService.update(input));
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

    @Test
    void shouldThrowWhenUpdateLodgingWithDuplicateEmail() {
        Lodging existing = new Lodging();
        existing.setId(1L);
        existing.setName("Hotel");
        existing.setEmail("original@test.com");

        LodgingDTO input = new LodgingDTO();
        input.setId(1L);
        input.setName("Hotel");
        input.setEmail("otro@test.com");

        when(lodgingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(lodgingRepository.existsByEmail("otro@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> lodgingService.update(input));
    }
}
