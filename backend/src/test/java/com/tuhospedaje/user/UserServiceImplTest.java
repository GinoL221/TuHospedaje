package com.tuhospedaje.user;

import com.tuhospedaje.dto.user.UserDTO;
import com.tuhospedaje.entity.AdminInvariantLock;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.exception.LastEnabledAdminException;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.AdminInvariantLockRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.RefreshSessionService;
import com.tuhospedaje.service.impl.UserServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminInvariantLockRepository adminInvariantLockRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private LodgingRepository lodgingRepository;

    @Mock
    private ObjectProvider<RefreshSessionService> refreshSessions;

    @Mock
    private RefreshSessionService refreshSessionService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldReturnAllUsers() {
        User userOne = new User();
        userOne.setId(1L);
        userOne.setFirstName("Juan");
        userOne.setLastName("Pérez");
        userOne.setEmail("juan@test.com");
        userOne.setRole(RoleEnum.USER);

        User userTwo = new User();
        userTwo.setId(2L);
        userTwo.setFirstName("Admin");
        userTwo.setLastName("Sistema");
        userTwo.setEmail("admin@test.com");
        userTwo.setRole(RoleEnum.ADMIN);

        when(userRepository.findAll()).thenReturn(List.of(userOne, userTwo));

        List<UserDTO> response = userService.findAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getEmail()).isEqualTo("juan@test.com");
        assertThat(response.get(1).getRole()).isEqualTo("ADMIN");
    }

    @Test
    void shouldUpdateUserRoleSuccessfully() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setFirstName("Juan");
        existingUser.setEmail("juan@test.com");
        existingUser.setRole(RoleEnum.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO response = userService.updateRole(1L, "ADMIN");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void shouldThrowWhenUpdateRoleOfNonExistentUser() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateRole(999L, "ADMIN"));
    }

    @Test
    void shouldRejectDemotionOfTheOnlyEnabledAdminWithoutSaving() {
        User onlyAdmin = buildAdmin(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(onlyAdmin));
        lockCoordinator();
        when(userRepository.lockEnabledAdminsInIdOrder()).thenReturn(List.of(onlyAdmin));

        assertThrows(LastEnabledAdminException.class, () -> userService.updateRole(1L, "USER"));

        assertThat(onlyAdmin.getRole()).isEqualTo(RoleEnum.ADMIN);
        verify(userRepository, never()).save(any(User.class));
        verify(refreshSessions, never()).getIfAvailable();
        assertCoordinatorPrecedesTargetAndEnabledAdminLocks(onlyAdmin);
    }

    @Test
    void shouldAllowDemotionWhenAnotherEnabledAdminExists() {
        User targetAdmin = buildAdmin(1L);
        User otherAdmin = buildAdmin(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(targetAdmin));
        lockCoordinator();
        when(userRepository.lockEnabledAdminsInIdOrder()).thenReturn(List.of(targetAdmin, otherAdmin));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO response = userService.updateRole(1L, "USER");

        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository).save(targetAdmin);
        verify(refreshSessions, never()).getIfAvailable();
        assertCoordinatorPrecedesTargetAndEnabledAdminLocks(targetAdmin);
    }

    @Test
    void shouldRejectDisablingTheOnlyEnabledAdminWithoutRevokingSessions() {
        User onlyAdmin = buildAdmin(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(onlyAdmin));
        lockCoordinator();
        when(userRepository.lockEnabledAdminsInIdOrder()).thenReturn(List.of(onlyAdmin));

        assertThrows(LastEnabledAdminException.class, () -> userService.setEnabled(1L, false));

        assertThat(onlyAdmin.isEnabled()).isTrue();
        verify(userRepository, never()).save(any(User.class));
        verify(refreshSessions, never()).getIfAvailable();
        assertCoordinatorPrecedesTargetAndEnabledAdminLocks(onlyAdmin);
    }

    @Test
    void shouldNotLockOrSaveForNoOpRoleOrEnabledUpdates() {
        User admin = buildAdmin(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        userService.updateRole(1L, "ADMIN");
        userService.setEnabled(1L, true);

        verify(adminInvariantLockRepository, never()).lockById(1L);
        verify(userRepository, never()).lockEnabledAdminsInIdOrder();
        verify(userRepository, never()).save(any(User.class));
        verify(refreshSessions, never()).getIfAvailable();
    }

    @Test
    void shouldSavePromotionAndReenableWithoutTakingTheEnabledAdminLock() {
        User disabledUser = buildUser(1L);
        disabledUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(disabledUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateRole(1L, "ADMIN");
        userService.setEnabled(1L, true);

        assertThat(disabledUser.getRole()).isEqualTo(RoleEnum.ADMIN);
        assertThat(disabledUser.isEnabled()).isTrue();
        verify(userRepository, times(2)).save(disabledUser);
        verify(adminInvariantLockRepository, never()).lockById(1L);
        verify(userRepository, never()).lockEnabledAdminsInIdOrder();
        verify(refreshSessions, never()).getIfAvailable();
    }

    @Test
    void shouldRevokeSessionsOnceForAnAllowedAdminDisable() {
        User targetAdmin = buildAdmin(1L);
        User otherAdmin = buildAdmin(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(targetAdmin));
        lockCoordinator();
        when(userRepository.lockEnabledAdminsInIdOrder()).thenReturn(List.of(targetAdmin, otherAdmin));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshSessions.getIfAvailable()).thenReturn(refreshSessionService);

        userService.setEnabled(1L, false);

        verify(userRepository).save(targetAdmin);
        verify(refreshSessionService).revokeAll(1L, "ADMIN");
        assertCoordinatorPrecedesTargetAndEnabledAdminLocks(targetAdmin);
    }

    @Test
    void shouldSkipCollectiveLockSaveAndRevocationWhenRefreshMakesDisableANoOp() {
        User targetAdmin = buildAdmin(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(targetAdmin));
        lockCoordinator();
        doAnswer(invocation -> {
            targetAdmin.setEnabled(false);
            return null;
        }).when(entityManager).refresh(targetAdmin, LockModeType.PESSIMISTIC_WRITE);

        userService.setEnabled(1L, false);

        verify(adminInvariantLockRepository).lockById(1L);
        verify(entityManager).refresh(targetAdmin, LockModeType.PESSIMISTIC_WRITE);
        verify(userRepository, never()).lockEnabledAdminsInIdOrder();
        verify(userRepository, never()).save(any(User.class));
        verify(refreshSessions, never()).getIfAvailable();
    }

    private void lockCoordinator() {
        when(adminInvariantLockRepository.lockById(1L)).thenReturn(Optional.of(mock(AdminInvariantLock.class)));
    }

    private void assertCoordinatorPrecedesTargetAndEnabledAdminLocks(User target) {
        InOrder inOrder = inOrder(adminInvariantLockRepository, entityManager, userRepository);
        inOrder.verify(adminInvariantLockRepository).lockById(1L);
        inOrder.verify(entityManager).refresh(target, LockModeType.PESSIMISTIC_WRITE);
        inOrder.verify(userRepository).lockEnabledAdminsInIdOrder();
    }

    // --- Favorite not-found branches ---

    @Test
    void addFavorite_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.addFavorite(999L, 1L)
        );
        assertThat(ex.getMessage()).isEqualTo("Usuario no encontrado");
    }

    @Test
    void addFavorite_whenLodgingNotFound_throwsResourceNotFoundException() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(lodgingRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.addFavorite(1L, 999L)
        );
        assertThat(ex.getMessage()).isEqualTo("Alojamiento no encontrado");
    }

    @Test
    void removeFavorite_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.removeFavorite(999L, 1L)
        );
        assertThat(ex.getMessage()).isEqualTo("Usuario no encontrado");
    }

    @Test
    void getFavorites_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getFavorites(999L)
        );
        assertThat(ex.getMessage()).isEqualTo("Usuario no encontrado");
    }

    // --- helpers ---

    private static User buildAdmin(Long id) {
        User user = buildUser(id);
        user.setRole(RoleEnum.ADMIN);
        user.setEnabled(true);
        return user;
    }

    private static User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test" + id + "@tuhospedaje.com");
        user.setPassword("secret");
        user.setRole(RoleEnum.USER);
        user.setFavorites(new HashSet<>());
        return user;
    }
}
