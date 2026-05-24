package com.tuhospedaje.dto;

import com.tuhospedaje.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
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
