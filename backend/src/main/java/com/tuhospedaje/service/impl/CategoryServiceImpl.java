package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.category.CategoryDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.exception.DuplicateResourceException;
import com.tuhospedaje.exception.LocalizedIllegalArgumentException;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.exception.UniqueConstraintViolationSupport;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.service.CategoryService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final String CATEGORY_NAME_CONSTRAINT = "UK_categories_name";

    private final CategoryRepository categoryRepository;
    private final LodgingRepository lodgingRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, LodgingRepository lodgingRepository) {
        this.categoryRepository = categoryRepository;
        this.lodgingRepository = lodgingRepository;
    }

    @Override
    @Transactional
    public CategoryDTO save(CategoryDTO dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("error.category.duplicate_name", dto.getName());
        }

        Category category = dto.toEntity();
        return persistAndTranslateDuplicate(category, dto.getName());
    }

    @Override
    @Transactional
    public CategoryDTO update(CategoryDTO dto) throws ResourceNotFoundException {
        Category category = categoryRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.getId()));

        Optional<Category> existingWithName = categoryRepository.findByNameIgnoreCase(dto.getName());
        if (existingWithName.isPresent() && !existingWithName.get().getId().equals(dto.getId())) {
            throw new DuplicateResourceException("error.category.duplicate_name", dto.getName());
        }

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setIcon(dto.getIcon());
        // A null imageUrl means the edit omitted media (controller-level validation
        // already rejected any non-null but invalid replacement before reaching here),
        // so the previously stored, legacy-nullable image is preserved unchanged.
        if (dto.getImageUrl() != null) {
            category.setImageUrl(dto.getImageUrl());
        }
        return persistAndTranslateDuplicate(category, dto.getName());
    }

    private CategoryDTO persistAndTranslateDuplicate(Category category, String candidateName) {
        try {
            Category saved = categoryRepository.saveAndFlush(category);
            return CategoryDTO.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            if (UniqueConstraintViolationSupport.matchesConstraint(ex, CATEGORY_NAME_CONSTRAINT)) {
                throw new DuplicateResourceException("error.category.duplicate_name", candidateName);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public Optional<CategoryDTO> delete(Long id) throws ResourceNotFoundException {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        long lodgingCount = lodgingRepository.countByCategoryId(id);
        if (lodgingCount > 0) {
            throw new LocalizedIllegalArgumentException("error.category.in_use", lodgingCount);
        }

        categoryRepository.deleteById(id);
        return Optional.of(CategoryDTO.fromEntity(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> findAll() {
        return categoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(CategoryDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryDTO> findById(Long id) {
        return categoryRepository.findById(id).map(CategoryDTO::fromEntity);
    }
}
