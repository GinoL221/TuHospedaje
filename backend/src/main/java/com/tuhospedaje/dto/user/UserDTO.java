package com.tuhospedaje.dto.user;

import com.tuhospedaje.entity.User;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Public representation of a registered user")
public class UserDTO {

    @Schema(description = "Unique identifier of the user", example = "1")
    private Long id;

    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Role assigned to the user (e.g. ADMIN, USER)", example = "USER")
    private String role;

    @Schema(description = "URL of the user's profile image", example = "https://res.cloudinary.com/demo/image/upload/sample.jpg", nullable = true)
    private String imageUrl;

    public static UserDTO fromEntity(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setImageUrl(user.getImageUrl());
        return dto;
    }
}
