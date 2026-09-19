package com.tuhospedaje.service;

import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;

public record AuthenticatedActor(long id, RoleEnum role, String firstName, String lastName, String email) {

    public static AuthenticatedActor from(User user) {
        return new AuthenticatedActor(
                user.getId(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }
}
