package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.user.UserDTO;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.RefreshSessionService;
import com.tuhospedaje.service.UserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LodgingRepository lodgingRepository;
    // ObjectProvider, NOT a hard constructor dependency (Design ADR-0): RefreshSessionService
    // has no bean at all when app.session.refresh.enabled=false (RefreshSessionConfiguration
    // is @ConditionalOnProperty). A hard dependency here would break ApplicationContext
    // startup with the flag off, defeating the documented rollback/kill-switch.
    private final ObjectProvider<RefreshSessionService> refreshSessions;

    public UserServiceImpl(UserRepository userRepository, LodgingRepository lodgingRepository,
            ObjectProvider<RefreshSessionService> refreshSessions) {
        this.userRepository = userRepository;
        this.lodgingRepository = lodgingRepository;
        this.refreshSessions = refreshSessions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public UserDTO updateRole(Long id, String newRole) throws ResourceNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        user.setRole(RoleEnum.valueOf(newRole));
        User updated = userRepository.save(user);
        return UserDTO.fromEntity(updated);
    }

    @Override
    @Transactional
    public UserDTO setEnabled(Long id, boolean enabled) throws ResourceNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        user.setEnabled(enabled);
        User updated = userRepository.save(user);
        if (!enabled) {
            RefreshSessionService sessions = refreshSessions.getIfAvailable();
            if (sessions != null) {
                sessions.revokeAll(id, "ADMIN");
            }
        }
        return UserDTO.fromEntity(updated);
    }

    @Override
    @Transactional
    public void addFavorite(Long userId, Long lodgingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Lodging lodging = lodgingRepository.findById(lodgingId)
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado"));
        user.getFavorites().add(lodging);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long lodgingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        user.getFavorites().removeIf(l -> l.getId().equals(lodgingId));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> getFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return user.getFavorites().stream()
                .map(LodgingDTO::fromEntity)
                .toList();
    }
}
