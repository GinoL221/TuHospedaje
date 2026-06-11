package com.tuhospedaje.dto.category;

import com.tuhospedaje.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryDTO {
    @Null(message = "El id debe ser nulo al crear")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String description;

    private String icon;

    public Category toEntity() {
        Category category = new Category();
        category.setName(this.name);
        category.setDescription(this.description);
        category.setIcon(this.icon);
        return category;
    }

    public static CategoryDTO fromEntity(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setIcon(category.getIcon());
        return dto;
    }
}
