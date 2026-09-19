package com.tuhospedaje.lodging;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.lodging.LodgingSearchResponse;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.PolicyRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.service.LodgingQueryService;
import com.tuhospedaje.service.impl.LodgingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LodgingServiceImplTest {

    @Mock private LodgingRepository lodgingRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private FeatureRepository featureRepository;
    @Mock private PolicyRepository policyRepository;
    @Mock private RatingRepository ratingRepository;
    @Mock private LodgingQueryService lodgingQueryService;

    @InjectMocks private LodgingServiceImpl lodgingService;

    @Test
    void shouldSaveLodgingSuccessfully() {
        LodgingDTO dto = lodgingDto("Gran Hotel", "hotel@test.com");
        Lodging saved = lodging(1L, "Gran Hotel");
        when(lodgingRepository.existsByName(dto.getName())).thenReturn(false);
        when(lodgingRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(lodgingRepository.save(any(Lodging.class))).thenReturn(saved);

        LodgingDTO response = lodgingService.save(dto);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Gran Hotel");
    }

    @Test
    void shouldThrowWhenSaveDuplicateName() {
        LodgingDTO dto = lodgingDto("Gran Hotel", "hotel@test.com");
        when(lodgingRepository.existsByName(dto.getName())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> lodgingService.save(dto));
    }

    @Test
    void shouldAssignCategoryAndRelationshipsOnSave() {
        Category category = new Category();
        category.setId(1L);
        Feature feature = new Feature();
        feature.setId(2L);
        Policy policy = new Policy();
        policy.setId(3L);
        LodgingDTO dto = lodgingDto("Gran Hotel", "hotel@test.com");
        dto.setCategoryId(1L);
        dto.setFeatureIds(Set.of(2L));
        dto.setPolicyIds(Set.of(3L));
        Lodging saved = lodging(1L, "Gran Hotel");
        saved.setCategory(category);
        saved.setFeatures(Set.of(feature));
        saved.setPolicies(Set.of(policy));
        when(lodgingRepository.existsByName(dto.getName())).thenReturn(false);
        when(lodgingRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(featureRepository.findAllById(Set.of(2L))).thenReturn(List.of(feature));
        when(policyRepository.findAllById(Set.of(3L))).thenReturn(List.of(policy));
        when(lodgingRepository.save(any(Lodging.class))).thenReturn(saved);

        LodgingDTO response = lodgingService.save(dto);

        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getFeatureIds()).containsExactly(2L);
        assertThat(response.getPolicyIds()).containsExactly(3L);
    }

    @Test
    void shouldUpdateLodgingSuccessfully() {
        Lodging existing = lodging(1L, "Hotel");
        existing.setEmail("old@test.com");
        LodgingDTO dto = lodgingDto("Hotel Boutique", "old@test.com");
        dto.setId(1L);
        dto.setPricePerNight(new BigDecimal("27500.50"));
        dto.setMaxGuests(7);
        when(lodgingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(lodgingRepository.save(any(Lodging.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LodgingDTO response = lodgingService.update(dto);

        assertThat(response.getName()).isEqualTo("Hotel Boutique");
        assertThat(response.getPricePerNight()).isEqualByComparingTo("27500.50");
        assertThat(response.getMaxGuests()).isEqualTo(7);
    }

    @Test
    void shouldRejectDuplicateEmailOnUpdate() {
        Lodging existing = lodging(1L, "Hotel");
        existing.setEmail("old@test.com");
        LodgingDTO dto = lodgingDto("Hotel", "new@test.com");
        dto.setId(1L);
        when(lodgingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(lodgingRepository.existsByEmail("new@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> lodgingService.update(dto));
    }

    @Test
    void shouldDeleteExistingLodging() {
        Lodging lodging = lodging(8L, "Temporal");
        when(lodgingRepository.findById(8L)).thenReturn(Optional.of(lodging));

        Optional<LodgingDTO> deleted = lodgingService.delete(8L);

        assertThat(deleted).isPresent();
        assertThat(deleted.get().getId()).isEqualTo(8L);
        verify(lodgingRepository).deleteById(8L);
    }

    @Test
    void shouldReturnLodgingByIdAndEmptyWhenMissing() {
        Lodging lodging = lodging(5L, "Hotel Central");
        when(lodgingRepository.findById(5L)).thenReturn(Optional.of(lodging));
        when(lodgingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(lodgingService.findById(5L)).map(LodgingDTO::getName).contains("Hotel Central");
        assertThat(lodgingService.findById(999L)).isEmpty();
    }

    @Test
    void shouldFindLodgingsByName() {
        when(lodgingRepository.findByNameContainingIgnoreCase("Boutique"))
                .thenReturn(List.of(lodging(1L, "Hotel Boutique")));

        List<LodgingDTO> response = lodgingService.findByName("Boutique");

        assertThat(response).extracting(LodgingDTO::getName).containsExactly("Hotel Boutique");
    }

    @Test
    void shouldDelegateListQueriesToQueryService() {
        List<LodgingDTO> expected = List.of(new LodgingDTO());
        when(lodgingQueryService.findAll()).thenReturn(expected);
        when(lodgingQueryService.findByCategory(3L)).thenReturn(expected);
        when(lodgingQueryService.findAllRandom()).thenReturn(expected);

        assertThat(lodgingService.findAll()).isSameAs(expected);
        assertThat(lodgingService.findByCategory(3L)).isSameAs(expected);
        assertThat(lodgingService.findAllRandom()).isSameAs(expected);

        verify(lodgingQueryService).findAll();
        verify(lodgingQueryService).findByCategory(3L);
        verify(lodgingQueryService).findAllRandom();
    }

    @Test
    void shouldDelegateSearchAndAvailabilityQueriesToQueryService() {
        LodgingSearchResponse expected = new LodgingSearchResponse(List.of(), 0, 0, 0, 0);
        LocalDate checkIn = LocalDate.of(2026, 1, 10);
        LocalDate checkOut = LocalDate.of(2026, 1, 12);
        when(lodgingQueryService.search("Mar del Plata", checkIn, checkOut, 2, List.of(1L), null, null, 0, 9))
                .thenReturn(expected);

        assertThat(lodgingService.search("Mar del Plata", checkIn, checkOut, 2, List.of(1L), null, null, 0, 9))
                .isSameAs(expected);

        verify(lodgingQueryService).search("Mar del Plata", checkIn, checkOut, 2, List.of(1L), null, null, 0, 9);
    }

    private LodgingDTO lodgingDto(String name, String email) {
        LodgingDTO dto = new LodgingDTO();
        dto.setName(name);
        dto.setAddress("Calle 123");
        dto.setCity("Ciudad");
        dto.setCountry("País");
        dto.setPhoneNumber("123456");
        dto.setEmail(email);
        return dto;
    }

    private Lodging lodging(Long id, String name) {
        Lodging lodging = new Lodging();
        lodging.setId(id);
        lodging.setName(name);
        return lodging;
    }
}
