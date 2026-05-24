package com.tuhospedaje.lodging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.LodgingDTO;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LodgingControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String adminAuthHeader;
    private String userAuthHeader;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .firstName("Admin")
                .lastName("Lodging")
                .email("admin-lodging-crud-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build();
        User savedAdmin = userRepository.save(admin);
        adminAuthHeader = "Bearer " + jwtService.generateToken(savedAdmin);

        User regularUser = User.builder()
                .firstName("User")
                .lastName("Lodging")
                .email("user-lodging-crud-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build();
        User savedUser = userRepository.save(regularUser);
        userAuthHeader = "Bearer " + jwtService.generateToken(savedUser);
    }

    @Test
    void shouldCreateLodgingSuccessfully() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "Hotel Test",
                "description", "Descripción",
                "address", "Calle 123",
                "city", "Ciudad",
                "country", "País",
                "phoneNumber", "123456789",
                "email", "hotel-test@tuhospedaje.com"
        );

        mockMvc.perform(post("/api/lodgings")
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Hotel Test"));
    }

    @Test
    void shouldReturnForbiddenWhenCreatingLodgingWithoutAuth() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "Sin Auth",
                "email", "noauth@test.com"
        );

        mockMvc.perform(post("/api/lodgings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingLodgingWithUserRole() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "User Role",
                "email", "userrole@test.com"
        );

        mockMvc.perform(post("/api/lodgings")
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListAllLodgings() throws Exception {
        createTestLodging("Hotel A", "a@test.com");
        createTestLodging("Hotel B", "b@test.com");

        mockMvc.perform(get("/api/lodgings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void shouldGetLodgingById() throws Exception {
        Long id = createTestLodging("Hotel Detalle", "detalle@test.com");

        mockMvc.perform(get("/api/lodgings/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hotel Detalle"));
    }

    @Test
    void shouldReturnNotFoundWhenLodgingByIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/lodgings/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnRandomLodgings() throws Exception {
        createTestLodging("Random 1", "r1@test.com");
        createTestLodging("Random 2", "r2@test.com");

        mockMvc.perform(get("/api/lodgings/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void shouldSearchLodgingsByName() throws Exception {
        createTestLodging("Hotel Boutique", "boutique@test.com");

        mockMvc.perform(get("/api/lodgings/search")
                        .param("query", "Boutique"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Hotel Boutique"));
    }

    @Test
    void shouldReturnPaginatedLodgings() throws Exception {
        createTestLodging("Page Lodging 1", "p1@test.com");
        createTestLodging("Page Lodging 2", "p2@test.com");

        mockMvc.perform(get("/api/lodgings")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings").isArray())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void shouldUpdateLodgingSuccessfully() throws Exception {
        Long id = createTestLodging("Original", "update@test.com");

        Map<String, Object> updateRequest = Map.of(
                "name", "Actualizado",
                "description", "Nueva descripción",
                "address", "Nueva dirección",
                "city", "Nueva ciudad",
                "country", "Nuevo país",
                "phoneNumber", "999999",
                "email", "update@test.com"
        );

        mockMvc.perform(put("/api/lodgings/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Actualizado"))
                .andExpect(jsonPath("$.description").value("Nueva descripción"));
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingLodgingWithoutAuth() throws Exception {
        mockMvc.perform(put("/api/lodgings/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hack"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenDeletingLodgingWithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/lodgings/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteLodgingSuccessfully() throws Exception {
        Long id = createTestLodging("To Delete", "delete@test.com");

        mockMvc.perform(delete("/api/lodgings/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lodgings/{id}", id))
                .andExpect(status().isNotFound());
    }

    private Long createTestLodging(String name, String email) throws Exception {
        Map<String, Object> request = Map.of(
                "name", name,
                "description", "Descripción",
                "address", "Calle 123",
                "city", "Ciudad",
                "country", "País",
                "phoneNumber", "123456789",
                "email", email
        );

        String response = mockMvc.perform(post("/api/lodgings")
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
