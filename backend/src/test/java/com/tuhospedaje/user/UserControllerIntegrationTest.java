package com.tuhospedaje.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.auth.RoleRequest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        String adminToken = jwtService.generateToken(savedAdmin);
        adminAuthHeader = "Bearer " + adminToken;

        User regularUser = User.builder()
                .firstName("Regular")
                .lastName("User")
                .email("regular-user-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build();

        User savedUser = userRepository.save(regularUser);
        String userToken = jwtService.generateToken(savedUser);
        userAuthHeader = "Bearer " + userToken;
        regularUserId = savedUser.getId();
    }

    @Test
    void shouldListUsersSuccessfully() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader))
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
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateUserRoleSuccessfully() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setRole("ADMIN");

        mockMvc.perform(put("/api/users/{id}/role", regularUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
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

        mockMvc.perform(put("/api/users/{id}/role", regularUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnNotFoundWhenUpdatingRoleOfNonExistentUser() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setRole("ADMIN");

        mockMvc.perform(put("/api/users/{id}/role", 999L)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingRoleWithoutToken() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setRole("ADMIN");

        mockMvc.perform(put("/api/users/{id}/role", regularUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingRoleWithUserRole() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setRole("ADMIN");

        mockMvc.perform(put("/api/users/{id}/role", regularUserId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
