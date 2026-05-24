package com.tuhospedaje.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.FeatureDTO;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeatureControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeatureRepository featureRepository;

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
                .lastName("Test")
                .email("admin-feature-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build();

        User savedAdmin = userRepository.save(admin);
        String adminToken = jwtService.generateToken(savedAdmin);
        adminAuthHeader = "Bearer " + adminToken;

        User regularUser = User.builder()
                .firstName("User")
                .lastName("Test")
                .email("user-feature-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build();

        User savedUser = userRepository.save(regularUser);
        String userToken = jwtService.generateToken(savedUser);
        userAuthHeader = "Bearer " + userToken;
    }

    @Test
    void shouldCreateFeatureSuccessfully() throws Exception {
        FeatureDTO request = new FeatureDTO();
        request.setName("FT-WiFi-Gratis");
        request.setIcon("wifi-icon");

        mockMvc.perform(post("/api/features")
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("FT-WiFi-Gratis"))
                .andExpect(jsonPath("$.icon").value("wifi-icon"));
    }

    @Test
    void shouldReturnBadRequestWhenCreatingDuplicateFeature() throws Exception {
        Feature existing = new Feature();
        existing.setName("FT-Cochera");
        existing.setIcon("car-icon");
        featureRepository.save(existing);

        FeatureDTO request = new FeatureDTO();
        request.setName("FT-Cochera");
        request.setIcon("car-icon");

        mockMvc.perform(post("/api/features")
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldListFeatures() throws Exception {
        Feature one = new Feature();
        one.setName("FT-WiFi");
        one.setIcon("wifi-icon");
        featureRepository.save(one);

        Feature two = new Feature();
        two.setName("FT-Cochera");
        two.setIcon("car-icon");
        featureRepository.save(two);

        mockMvc.perform(get("/api/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void shouldGetFeatureById() throws Exception {
        Feature feature = new Feature();
        feature.setName("FT-Aire-Acondicionado");
        feature.setIcon("ac-icon");
        Feature saved = featureRepository.save(feature);

        mockMvc.perform(get("/api/features/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("FT-Aire-Acondicionado"));
    }

    @Test
    void shouldReturnNotFoundWhenGettingFeatureByIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/features/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateFeatureSuccessfully() throws Exception {
        Feature feature = new Feature();
        feature.setName("FT-WiFi-Basico");
        feature.setIcon("old-icon");
        Feature saved = featureRepository.save(feature);

        FeatureDTO request = new FeatureDTO();
        request.setName("FT-WiFi-Premium");
        request.setIcon("new-icon");

        mockMvc.perform(put("/api/features/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("FT-WiFi-Premium"))
                .andExpect(jsonPath("$.icon").value("new-icon"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingFeatureDoesNotExist() throws Exception {
        FeatureDTO request = new FeatureDTO();
        request.setName("FT-No-Existe");
        request.setIcon("none");

        mockMvc.perform(put("/api/features/{id}", 888L)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteFeatureSuccessfully() throws Exception {
        Feature feature = new Feature();
        feature.setName("FT-Temporal");
        feature.setIcon("temp-icon");
        Feature saved = featureRepository.save(feature);

        mockMvc.perform(delete("/api/features/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingFeatureDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/features/{id}", 777L)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingFeatureWithoutToken() throws Exception {
        FeatureDTO request = new FeatureDTO();
        request.setName("FT-Sin-Auth");
        request.setIcon("lock-icon");

        mockMvc.perform(post("/api/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingFeatureWithUserRole() throws Exception {
        FeatureDTO request = new FeatureDTO();
        request.setName("FT-No-Admin");
        request.setIcon("user-icon");

        mockMvc.perform(post("/api/features")
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
