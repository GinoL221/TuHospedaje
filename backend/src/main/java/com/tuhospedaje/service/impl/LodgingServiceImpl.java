package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.LodgingDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.service.LodgingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class LodgingServiceImpl implements LodgingService {

    private static final int RANDOM_POOL_SIZE = 100;
    private static final int RANDOM_RESULT_SIZE = 10;

    private final LodgingRepository lodgingRepository;
    private final CategoryRepository categoryRepository;
    private final FeatureRepository featureRepository;

    public LodgingServiceImpl(LodgingRepository lodgingRepository, CategoryRepository categoryRepository, FeatureRepository featureRepository) {
        this.lodgingRepository = lodgingRepository;
        this.categoryRepository = categoryRepository;
        this.featureRepository = featureRepository;
    }

    @Override
    public LodgingDTO save(LodgingDTO dto) {
        if (lodgingRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Ya existe un alojamiento con el nombre: " + dto.getName());
        }
        if (dto.getEmail() != null && lodgingRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un alojamiento con el email: " + dto.getEmail());
        }
        Lodging lodging = dto.toEntity();
        lodging.setCategory(resolveCategory(dto.getCategoryId()));
        lodging.setFeatures(resolveFeatures(dto.getFeatureIds()));
        Lodging saved = lodgingRepository.save(lodging);
        return LodgingDTO.fromEntity(saved);
    }

    @Override
    public LodgingDTO update(LodgingDTO dto) throws ResourceNotFoundException {
        Lodging lodging = lodgingRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado con ID: " + dto.getId()));
        // Si el email cambió, validar que no esté duplicado
        if (dto.getEmail() != null && !dto.getEmail().equals(lodging.getEmail())
                && lodgingRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un alojamiento con el email: " + dto.getEmail());
        }
        lodging.setName(dto.getName());
        lodging.setDescription(dto.getDescription());
        lodging.setAddress(dto.getAddress());
        lodging.setCity(dto.getCity());
        lodging.setCountry(dto.getCountry());
        lodging.setPhoneNumber(dto.getPhoneNumber());
        lodging.setEmail(dto.getEmail());
        lodging.setCategory(resolveCategory(dto.getCategoryId()));
        lodging.setFeatures(resolveFeatures(dto.getFeatureIds()));
        Lodging updated = lodgingRepository.save(lodging);
        return LodgingDTO.fromEntity(updated);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }

    private Set<Feature> resolveFeatures(Set<Long> featureIds) {
        if (featureIds == null || featureIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(featureRepository.findAllById(featureIds));
    }

    @Override
    public Optional<LodgingDTO> delete(Long id) throws ResourceNotFoundException {
        Lodging lodging = lodgingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado con ID: " + id));
        lodgingRepository.deleteById(id);
        return Optional.of(LodgingDTO.fromEntity(lodging));
    }

    @Override
    public List<LodgingDTO> findAll() {
        return lodgingRepository.findAll()
                .stream()
                .map(LodgingDTO::fromEntity)
                .toList();
    }

    @Override
    public Optional<LodgingDTO> findById(Long id) {
        return lodgingRepository.findById(id).map(LodgingDTO::fromEntity);
    }

    @Override
    public List<LodgingDTO> findByName(String name) {
        return lodgingRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(LodgingDTO::fromEntity)
                .toList();
    }

    @Override
    public List<LodgingDTO> findByCategory(Long categoryId) {
        return lodgingRepository.findByCategoryId(categoryId)
                .stream()
                .map(LodgingDTO::fromEntity)
                .toList();
    }

    @Override
    public Map<String, Object> findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Lodging> lodgingPage = lodgingRepository.findAll(pageable);
        List<LodgingDTO> lodgings = lodgingPage.getContent().stream()
                .map(LodgingDTO::fromEntity)
                .toList();
        Map<String, Object> response = new HashMap<>();
        response.put("lodgings", lodgings);
        response.put("currentPage", lodgingPage.getNumber());
        response.put("totalItems", lodgingPage.getTotalElements());
        response.put("totalPages", lodgingPage.getTotalPages());
        return response;
    }

    @Override
    public List<LodgingDTO> findAllRandom() {
        long total = lodgingRepository.count();
        if (total == 0) return List.of();

        // Toma un pool acotado para evitar sobrecargar memoria si la BD crece mucho
        int fetchSize = (int) Math.min(total, RANDOM_POOL_SIZE);
        List<Lodging> pool = new ArrayList<>(lodgingRepository.findAll(PageRequest.of(0, fetchSize)).getContent());
        Collections.shuffle(pool);
        return pool.stream()
                .limit(RANDOM_RESULT_SIZE)
                .map(LodgingDTO::fromEntity)
                .toList();
    }
}
