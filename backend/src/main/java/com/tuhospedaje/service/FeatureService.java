package com.tuhospedaje.service;

import com.tuhospedaje.dto.features.FeatureDTO;
import com.tuhospedaje.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

public interface FeatureService {
    FeatureDTO save(FeatureDTO dto);

    FeatureDTO update(FeatureDTO dto) throws ResourceNotFoundException;

    Optional<FeatureDTO> delete(Long id) throws ResourceNotFoundException;

    List<FeatureDTO> findAll();

    Optional<FeatureDTO> findById(Long id);
}
