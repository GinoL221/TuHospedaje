package com.tuhospedaje.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.auth.RoleRequest;
import com.tuhospedaje.dto.auth.UserStatusRequest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String adminAuthHeader;
    private String userAuthHeader;
    private Long regularUserId;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .firstName("Admin")
                .lastName("Test")
                .email("admin-user-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build();

        User savedAdmin = userRepository.save(admin);
        adminAuthHeader = jwtService.generateToken(savedAdmin);

        User regularUser = User.builder()
                .firstName("Regular")
                .lastName("User")
                .email("regular-user-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build();

        User savedUser = userRepository.save(regularUser);
        userAuthHeader = jwtService.generateToken(savedUser);
        regularUserId = savedUser.getId();
    }

    @Test
    void shouldListUsersSuccessfully() throws Exception {
        mockMvc.perform(get("/api/users")
                        .cookie(accessCookie(adminAuthHeader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].email").exists());
    }

    @Test
    void shouldReturnForbiddenWhenListingUsersWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenListingUsersWithUserRole() throws Exception {
        mockMvc.perform(get("/api/users")
                        .cookie(accessCookie(userAuthHeader)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateUserRoleSuccessfully() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setRole("ADMIN");

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(put("/api/users/{id}/role", regularUserId)
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(regularUserId))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingRoleWithBlankRole() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setRole("  ");

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(put("/api/users/{id}/role", regularUserId)
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnNotFoundWhenUpdatingRoleOfNonExistentUser() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setRole("ADMIN");

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(put("/api/users/{id}/role", 999L)
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingRoleWithoutToken() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setRole("ADMIN");

        // Keep CSRF valid even without auth, so the 403 is attributable to the missing
        // token, not to a missing CSRF header (design's explicit ordering-trap warning).
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(put("/api/users/{id}/role", regularUserId)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingRoleWithUserRole() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setRole("ADMIN");

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(put("/api/users/{id}/role", regularUserId)
                        .cookie(accessCookie(userAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ADR-0 (conditional-bean kill-switch): this test class runs with the DEFAULT test
    // properties, where app.session.refresh.enabled=false, so no RefreshSessionService bean
    // exists. If UserServiceImpl depended on it as a hard constructor dependency instead of
    // ObjectProvider<RefreshSessionService>, the whole Spring context above would fail to
    // start and EVERY test in this class would fail before this assertion ever ran. The
    // context starting AND the disable call succeeding together prove the kill-switch:
    // admin-disable still flips the enabled flag and no-ops the (absent) revokeAll call.
    @Test
    void shouldDisableUserSuccessfullyEvenWithRefreshSessionsDisabled() throws Exception {
        UserStatusRequest request = new UserStatusRequest(false);

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(patch("/api/users/{id}/enabled", regularUserId)
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(regularUserId).orElseThrow().isEnabled()).isFalse();
    }
}
