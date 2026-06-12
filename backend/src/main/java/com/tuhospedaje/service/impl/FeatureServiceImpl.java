package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.features.FeatureDTO;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.service.FeatureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class FeatureServiceImpl implements FeatureService {

    private final FeatureRepository featureRepository;

    public FeatureServiceImpl(FeatureRepository featureRepository) {
        this.featureRepository = featureRepository;
    }

    @Override
    @Transactional
    public FeatureDTO save(FeatureDTO dto) {
        if (featureRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Ya existe una característica con el nombre: " + dto.getName());
        }
        Feature feature = dto.toEntity();
        Feature saved = featureRepository.save(feature);
        return FeatureDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public FeatureDTO update(FeatureDTO dto) throws ResourceNotFoundException {
        Feature feature = featureRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Característica no encontrada con ID: " + dto.getId()));
        Optional<Feature> existingWithName = Optional.ofNullable(featureRepository.findByNameIgnoreCase(dto.getName()));
        if (existingWithName.isPresent() && !existingWithName.get().getId().equals(dto.getId())) {
            throw new IllegalArgumentException("Ya existe una característica con el nombre: " + dto.getName());
        }
        feature.setName(dto.getName());
        feature.setIcon(dto.getIcon());
        Feature updated = featureRepository.save(feature);
        return FeatureDTO.fromEntity(updated);
    }

    @Override
    @Transactional
    public Optional<FeatureDTO> delete(Long id) throws ResourceNotFoundException {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Característica no encontrada con ID: " + id));
        featureRepository.deleteById(id);
        return Optional.of(FeatureDTO.fromEntity(feature));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureDTO> findAll() {
        return featureRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Feature::getName, String.CASE_INSENSITIVE_ORDER))
                .map(FeatureDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FeatureDTO> findById(Long id) {
        return featureRepository.findById(id).map(FeatureDTO::fromEntity);
    }
}
