package com.tuhospedaje.dto.category;

import com.tuhospedaje.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Lodging category data transfer object")
public class CategoryDTO {

    @Null(message = "El id debe ser nulo al crear")
    @Schema(description = "Unique identifier of the category (null on create)", example = "3")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Name of the category", example = "Cabin")
    private String name;

    @Schema(description = "Short description of the category", example = "Rustic cabins ideal for nature retreats and mountain getaways.")
    private String description;

    @Schema(description = "Icon identifier or URL representing the category", example = "fa-house-chimney")
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
