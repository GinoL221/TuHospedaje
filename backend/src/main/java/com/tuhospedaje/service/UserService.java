package com.tuhospedaje.service;

import com.tuhospedaje.dto.user.UserDTO;
import com.tuhospedaje.exception.ResourceNotFoundException;

import java.util.List;

public interface UserService {
    List<UserDTO> findAll();

    UserDTO updateRole(Long id, String newRole) throws ResourceNotFoundException;
}
