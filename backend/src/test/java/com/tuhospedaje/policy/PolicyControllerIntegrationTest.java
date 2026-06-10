package com.tuhospedaje.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.dto.policy.PolicyDTO;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.PolicyRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
@Import(TestcontainersConfiguration.class)
class PolicyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private JwtService jwtService;

    private String adminAuthHeader;
    private String userAuthHeader;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .firstName("Admin")
                .lastName("Policy")
                .email("admin-policy-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build();

        User savedAdmin = userRepository.save(admin);
        String adminToken = jwtService.generateToken(savedAdmin);
        adminAuthHeader = "Bearer " + adminToken;

        User regularUser = User.builder()
                .firstName("User")
                .lastName("Policy")
                .email("user-policy-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build();

        User savedUser = userRepository.save(regularUser);
        String userToken = jwtService.generateToken(savedUser);
        userAuthHeader = "Bearer " + userToken;
    }

    @AfterEach
    void tearDown() {
        // Seeded ratings and reservations reference users, so they must go first
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldListPoliciesPublicly() throws Exception {
        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void shouldGetPolicyByIdPublicly() throws Exception {
        Policy policy = new Policy();
        policy.setName("PL-Check-in");
        policy.setDescription("Desde las 14:00");
        policy.setIcon("fa-solid fa-clock");
        Policy saved = policyRepository.save(policy);

        mockMvc.perform(get("/api/policies/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("PL-Check-in"));
    }

    @Test
    void shouldReturnNotFoundWhenGettingPolicyByIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/policies/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreatePolicySuccessfully() throws Exception {
        PolicyDTO request = new PolicyDTO();
        request.setName("PL-Early-Check-in");
        request.setDescription("Disponible desde las 10:00");
        request.setIcon("fa-solid fa-sun");

        mockMvc.perform(post("/api/policies")
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("PL-Early-Check-in"));
    }

    @Test
    void shouldReturnBadRequestWhenCreatingDuplicatePolicy() throws Exception {
        Policy existing = new Policy();
        existing.setName("PL-Duplicate");
        existing.setDescription("Duplicada");
        existing.setIcon("fa-solid fa-ban");
        policyRepository.save(existing);

        PolicyDTO request = new PolicyDTO();
        request.setName("PL-Duplicate");
        request.setDescription("Duplicada");
        request.setIcon("fa-solid fa-ban");

        mockMvc.perform(post("/api/policies")
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnBadRequestWhenCreatingPolicyWithInvalidBody() throws Exception {
        PolicyDTO request = new PolicyDTO();
        request.setName("");
        request.setDescription("Inválida");
        request.setIcon("");

        mockMvc.perform(post("/api/policies")
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldUpdatePolicySuccessfully() throws Exception {
        Policy policy = new Policy();
        policy.setName("PL-Update-Base");
        policy.setDescription("Base");
        policy.setIcon("fa-solid fa-clock");
        Policy saved = policyRepository.save(policy);

        PolicyDTO request = new PolicyDTO();
        request.setName("PL-Update-New");
        request.setDescription("Nueva descripción");
        request.setIcon("fa-solid fa-clock");

        mockMvc.perform(put("/api/policies/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("PL-Update-New"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingPolicyDoesNotExist() throws Exception {
        PolicyDTO request = new PolicyDTO();
        request.setName("PL-No-Existe");
        request.setDescription("none");
        request.setIcon("fa-solid fa-ban");

        mockMvc.perform(put("/api/policies/{id}", 888888L)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeletePolicySuccessfully() throws Exception {
        Policy policy = new Policy();
        policy.setName("PL-Temporal");
        policy.setDescription("Temporal");
        policy.setIcon("fa-solid fa-trash");
        Policy saved = policyRepository.save(policy);

        mockMvc.perform(delete("/api/policies/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnBadRequestWhenDeletingPolicyReferencedByLodging() throws Exception {
        mockMvc.perform(delete("/api/policies/{id}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingPolicyDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/policies/{id}", 777777L)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingPolicyWithoutToken() throws Exception {
        PolicyDTO request = new PolicyDTO();
        request.setName("PL-Sin-Auth");
        request.setDescription("Sin auth");
        request.setIcon("fa-solid fa-lock");

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingPolicyWithUserRole() throws Exception {
        PolicyDTO request = new PolicyDTO();
        request.setName("PL-No-Admin");
        request.setDescription("No admin");
        request.setIcon("fa-solid fa-user");

        mockMvc.perform(post("/api/policies")
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
