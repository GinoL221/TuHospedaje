package com.tuhospedaje.service;

import com.tuhospedaje.dto.CategoryDTO;
import com.tuhospedaje.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    CategoryDTO save(CategoryDTO categoryDTO);

    CategoryDTO update(CategoryDTO categoryDTO) throws ResourceNotFoundException;

    Optional<CategoryDTO> delete(Long id) throws ResourceNotFoundException;

    List<CategoryDTO> findAll();

    Optional<CategoryDTO> findById(Long id);
}
