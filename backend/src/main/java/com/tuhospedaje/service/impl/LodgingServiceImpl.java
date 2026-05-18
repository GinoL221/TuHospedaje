package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.LodgingDTO;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.service.ILodgingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LodgingServiceImpl implements ILodgingService {

    private static final int RANDOM_POOL_SIZE = 100;
    private static final int RANDOM_RESULT_SIZE = 10;

    private final LodgingRepository lodgingRepository;

    public LodgingServiceImpl(LodgingRepository lodgingRepository) {
        this.lodgingRepository = lodgingRepository;
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
        Lodging saved = lodgingRepository.save(lodging);
        return LodgingDTO.fromEntity(saved);
    }

    @Override
    public LodgingDTO update(LodgingDTO dto) throws ResourceNotFoundException {
        Lodging lodging = lodgingRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado con ID: " + dto.getId()));
        lodging.setName(dto.getName());
        lodging.setDescription(dto.getDescription());
        lodging.setAddress(dto.getAddress());
        lodging.setCity(dto.getCity());
        lodging.setCountry(dto.getCountry());
        lodging.setPhoneNumber(dto.getPhoneNumber());
        lodging.setEmail(dto.getEmail());
        Lodging updated = lodgingRepository.save(lodging);
        return LodgingDTO.fromEntity(updated);
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
