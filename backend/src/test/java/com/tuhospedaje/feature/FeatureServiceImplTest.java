package com.tuhospedaje.feature;

import com.tuhospedaje.dto.features.FeatureDTO;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.service.impl.FeatureServiceImpl;
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
class FeatureServiceImplTest {

    @Mock
    private FeatureRepository featureRepository;

    @InjectMocks
    private FeatureServiceImpl featureService;

    @Test
    void shouldCreateFeatureSuccessfully() {
        FeatureDTO dto = new FeatureDTO();
        dto.setName("WiFi");
        dto.setIcon("wifi-icon");

        Feature savedEntity = new Feature();
        savedEntity.setId(1L);
        savedEntity.setName("WiFi");
        savedEntity.setIcon("wifi-icon");

        when(featureRepository.existsByNameIgnoreCase("WiFi")).thenReturn(false);
        when(featureRepository.save(any(Feature.class))).thenReturn(savedEntity);

        FeatureDTO response = featureService.save(dto);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("WiFi");
        assertThat(response.getIcon()).isEqualTo("wifi-icon");
    }

    @Test
    void shouldThrowWhenCreateFeatureNameAlreadyExists() {
        FeatureDTO dto = new FeatureDTO();
        dto.setName("WiFi");
        dto.setIcon("wifi-icon");

        when(featureRepository.existsByNameIgnoreCase("WiFi")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> featureService.save(dto));
    }

    @Test
    void shouldReturnAllFeatures() {
        Feature featureOne = new Feature();
        featureOne.setId(1L);
        featureOne.setName("WiFi");
        featureOne.setIcon("wifi-icon");

        Feature featureTwo = new Feature();
        featureTwo.setId(2L);
        featureTwo.setName("Estacionamiento");
        featureTwo.setIcon("car-icon");

        when(featureRepository.findAll()).thenReturn(List.of(featureOne, featureTwo));

        List<FeatureDTO> response = featureService.findAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("Estacionamiento");
        assertThat(response.get(1).getIcon()).isEqualTo("wifi-icon");
    }

    @Test
    void shouldReturnFeatureById() {
        Feature feature = new Feature();
        feature.setId(9L);
        feature.setName("Piscina");
        feature.setIcon("pool-icon");

        when(featureRepository.findById(9L)).thenReturn(Optional.of(feature));

        Optional<FeatureDTO> response = featureService.findById(9L);

        assertThat(response).isPresent();
        assertThat(response.get().getId()).isEqualTo(9L);
        assertThat(response.get().getName()).isEqualTo("Piscina");
    }

    @Test
    void shouldReturnEmptyWhenFeatureByIdDoesNotExist() {
        when(featureRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<FeatureDTO> response = featureService.findById(999L);

        assertThat(response).isEmpty();
    }

    @Test
    void shouldUpdateFeatureSuccessfully() {
        Feature existing = new Feature();
        existing.setId(1L);
        existing.setName("WiFi");
        existing.setIcon("old-icon");

        FeatureDTO input = new FeatureDTO();
        input.setId(1L);
        input.setName("WiFi Premium");
        input.setIcon("new-icon");

        when(featureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(featureRepository.findByNameIgnoreCase("WiFi Premium")).thenReturn(null);
        when(featureRepository.save(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeatureDTO response = featureService.update(input);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("WiFi Premium");
        assertThat(response.getIcon()).isEqualTo("new-icon");
    }

    @Test
    void shouldThrowWhenUpdateFeatureDoesNotExist() {
        FeatureDTO input = new FeatureDTO();
        input.setId(777L);
        input.setName("No existe");
        input.setIcon("none");

        when(featureRepository.findById(777L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> featureService.update(input));
    }

    @Test
    void shouldThrowWhenUpdateFeatureNameAlreadyExists() {
        Feature existing = new Feature();
        existing.setId(1L);
        existing.setName("WiFi");
        existing.setIcon("old-icon");

        Feature conflicting = new Feature();
        conflicting.setId(2L);
        conflicting.setName("WiFi Premium");
        conflicting.setIcon("other-icon");

        FeatureDTO input = new FeatureDTO();
        input.setId(1L);
        input.setName("WiFi Premium");
        input.setIcon("new-icon");

        when(featureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(featureRepository.findByNameIgnoreCase("WiFi Premium")).thenReturn(conflicting);

        assertThrows(IllegalArgumentException.class, () -> featureService.update(input));
    }

    @Test
    void shouldDeleteFeatureSuccessfully() {
        Feature feature = new Feature();
        feature.setId(8L);
        feature.setName("Temporal");
        feature.setIcon("temp");

        when(featureRepository.findById(8L)).thenReturn(Optional.of(feature));

        Optional<FeatureDTO> deleted = featureService.delete(8L);

        assertThat(deleted).isPresent();
        assertThat(deleted.get().getId()).isEqualTo(8L);
    }

    @Test
    void shouldThrowWhenDeleteFeatureDoesNotExist() {
        when(featureRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> featureService.delete(404L));
    }
}
