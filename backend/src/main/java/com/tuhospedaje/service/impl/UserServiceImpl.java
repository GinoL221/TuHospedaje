package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.user.UserDTO;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.exception.LastEnabledAdminException;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.AdminInvariantLockRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.RefreshSessionService;
import com.tuhospedaje.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AdminInvariantLockRepository adminInvariantLockRepository;
    private final EntityManager entityManager;
    private final LodgingRepository lodgingRepository;
    // ObjectProvider, NOT a hard constructor dependency (Design ADR-0): RefreshSessionService
    // has no bean at all when app.session.refresh.enabled=false (RefreshSessionConfiguration
    // is @ConditionalOnProperty). A hard dependency here would break ApplicationContext
    // startup with the flag off, defeating the documented rollback/kill-switch.
    private final ObjectProvider<RefreshSessionService> refreshSessions;

    public UserServiceImpl(UserRepository userRepository, AdminInvariantLockRepository adminInvariantLockRepository,
            EntityManager entityManager, LodgingRepository lodgingRepository,
            ObjectProvider<RefreshSessionService> refreshSessions) {
        this.userRepository = userRepository;
        this.adminInvariantLockRepository = adminInvariantLockRepository;
        this.entityManager = entityManager;
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
        RoleEnum requestedRole = RoleEnum.valueOf(newRole);
        if (user.getRole() == requestedRole) {
            return UserDTO.fromEntity(user);
        }
        if (removesEnabledAdmin(user, requestedRole != RoleEnum.ADMIN)) {
            lockAndRefreshTarget(user);
            if (user.getRole() == requestedRole) {
                return UserDTO.fromEntity(user);
            }
            if (removesEnabledAdmin(user, requestedRole != RoleEnum.ADMIN)) {
                rejectIfLastEnabledAdmin(user);
            }
        }
        user.setRole(requestedRole);
        return UserDTO.fromEntity(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDTO setEnabled(Long id, boolean enabled) throws ResourceNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        if (user.isEnabled() == enabled) {
            return UserDTO.fromEntity(user);
        }
        if (removesEnabledAdmin(user, !enabled)) {
            lockAndRefreshTarget(user);
            if (user.isEnabled() == enabled) {
                return UserDTO.fromEntity(user);
            }
            if (removesEnabledAdmin(user, !enabled)) {
                rejectIfLastEnabledAdmin(user);
            }
        }
        boolean disablingNow = user.isEnabled() && !enabled;
        user.setEnabled(enabled);
        User updated = userRepository.save(user);
        if (disablingNow) {
            RefreshSessionService sessions = refreshSessions.getIfAvailable();
            if (sessions != null) {
                sessions.revokeAll(id, "ADMIN");
            }
        }
        return UserDTO.fromEntity(updated);
    }

    private boolean removesEnabledAdmin(User user, boolean removesAdminRoleOrEnabledState) {
        return removesAdminRoleOrEnabledState && user.getRole() == RoleEnum.ADMIN && user.isEnabled();
    }

    private void lockAndRefreshTarget(User user) {
        adminInvariantLockRepository.lockById(1L)
                .orElseThrow(() -> new IllegalStateException("Missing admin invariant lock row"));
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
    }

    private void rejectIfLastEnabledAdmin(User user) {
        List<User> enabledAdmins = userRepository.lockEnabledAdminsInIdOrder();
        if (enabledAdmins.size() == 1
                && enabledAdmins.stream().anyMatch(admin -> admin.getId().equals(user.getId()))) {
            throw new LastEnabledAdminException();
        }
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
