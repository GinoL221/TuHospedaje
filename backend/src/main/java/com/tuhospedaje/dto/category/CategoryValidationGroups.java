package com.tuhospedaje.dto.category;

import jakarta.validation.groups.Default;

/**
 * Bean Validation groups distinguishing category creation from category editing, so
 * representative-image validation can require a value on create while permitting
 * omission on update (preserving previously saved, legacy-nullable media). Both groups
 * extend {@link Default} so pre-existing unqualified constraints (e.g. {@code @NotBlank}
 * on {@code name}) keep being enforced when either group is selected explicitly.
 */
public final class CategoryValidationGroups {

    private CategoryValidationGroups() {
    }

    public interface OnCreate extends Default {
    }

    public interface OnUpdate extends Default {
    }
}
