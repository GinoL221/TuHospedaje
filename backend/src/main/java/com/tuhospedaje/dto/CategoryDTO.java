package com.tuhospedaje.dto;

import com.tuhospedaje.entity.Category;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryDTO {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String description;

    public Category toEntity() {
        Category category = new Category();
        category.setName(this.name);
        category.setDescription(this.description);
        return category;
    }

    public static CategoryDTO fromEntity(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        return dto;
    }
}
