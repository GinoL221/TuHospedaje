package com.tuhospedaje.user;

import com.tuhospedaje.dto.user.UserDTO;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LodgingRepository lodgingRepository;

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
