package com.tuhospedaje.service;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.user.UserDTO;
import com.tuhospedaje.exception.ResourceNotFoundException;

import java.util.List;

public interface UserService {

    List<UserDTO> findAll();

    UserDTO updateRole(Long id, String newRole) throws ResourceNotFoundException;

    UserDTO setEnabled(Long id, boolean enabled) throws ResourceNotFoundException;

    void addFavorite(Long userId, Long lodgingId);

    void removeFavorite(Long userId, Long lodgingId);

    List<LodgingDTO> getFavorites(Long userId);
}
