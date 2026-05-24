package com.tuhospedaje.service;

import com.tuhospedaje.dto.LodgingDTO;
import com.tuhospedaje.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LodgingService {
    LodgingDTO save(LodgingDTO lodgingDTO);

    LodgingDTO update(LodgingDTO lodgingDTO) throws ResourceNotFoundException;

    Optional<LodgingDTO> delete(Long id) throws ResourceNotFoundException;

    List<LodgingDTO> findAll();

    Optional<LodgingDTO> findById(Long id);

    List<LodgingDTO> findByName(String name);

    List<LodgingDTO> findByCategory(Long categoryId);

    Map<String, Object> findAllPaginated(int page, int size);

    List<LodgingDTO> findAllRandom();
}
