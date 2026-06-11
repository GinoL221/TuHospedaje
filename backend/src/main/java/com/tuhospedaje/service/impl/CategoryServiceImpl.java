package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.category.CategoryDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final LodgingRepository lodgingRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, LodgingRepository lodgingRepository) {
        this.categoryRepository = categoryRepository;
        this.lodgingRepository = lodgingRepository;
    }

    @Override
    public CategoryDTO save(CategoryDTO dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Category name already exists: " + dto.getName());
        }

        Category category = dto.toEntity();
        Category saved = categoryRepository.save(category);
        return CategoryDTO.fromEntity(saved);
    }

    @Override
    public CategoryDTO update(CategoryDTO dto) throws ResourceNotFoundException {
        Category category = categoryRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.getId()));

        Optional<Category> existingWithName = categoryRepository.findByNameIgnoreCase(dto.getName());
        if (existingWithName.isPresent() && !existingWithName.get().getId().equals(dto.getId())) {
            throw new IllegalArgumentException("Category name already exists: " + dto.getName());
        }

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setIcon(dto.getIcon());
        Category updated = categoryRepository.save(category);
        return CategoryDTO.fromEntity(updated);
    }

    @Override
    public Optional<CategoryDTO> delete(Long id) throws ResourceNotFoundException {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        long lodgingCount = lodgingRepository.countByCategoryId(id);
        if (lodgingCount > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar la categoría: " + lodgingCount + " alojamiento(s) la están usando"
            );
        }

        categoryRepository.deleteById(id);
        return Optional.of(CategoryDTO.fromEntity(category));
    }

    @Override
    public List<CategoryDTO> findAll() {
        return categoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(CategoryDTO::fromEntity)
                .toList();
    }

    @Override
    public Optional<CategoryDTO> findById(Long id) {
        return categoryRepository.findById(id).map(CategoryDTO::fromEntity);
    }
}
