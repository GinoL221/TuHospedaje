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
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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
        adminAuthHeader = jwtService.generateToken(savedAdmin);

        User regularUser = User.builder()
                .firstName("User")
                .lastName("Policy")
                .email("user-policy-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build();

        User savedUser = userRepository.save(regularUser);
        userAuthHeader = jwtService.generateToken(savedUser);
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

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(post("/api/policies")
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
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

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(post("/api/policies")
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
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

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(post("/api/policies")
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
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

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(put("/api/policies/{id}", saved.getId())
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
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

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(put("/api/policies/{id}", 888888L)
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
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

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(delete("/api/policies/{id}", saved.getId())
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnBadRequestWhenDeletingPolicyReferencedByLodging() throws Exception {
        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(delete("/api/policies/{id}", 1L)
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingPolicyDoesNotExist() throws Exception {
        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(delete("/api/policies/{id}", 777777L)
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingPolicyWithoutToken() throws Exception {
        PolicyDTO request = new PolicyDTO();
        request.setName("PL-Sin-Auth");
        request.setDescription("Sin auth");
        request.setIcon("fa-solid fa-lock");

        // Keep CSRF valid even without auth, so the 403 is attributable to the missing
        // token, not to a missing CSRF header (design's explicit ordering-trap warning).
        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(post("/api/policies")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
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

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(post("/api/policies")
                        .cookie(accessCookie(userAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /**
     * Obtains a real {@code XSRF-TOKEN} cookie via a CSRF-safe public GET, WITHOUT using
     * Spring Security Test's {@code .with(csrf())} post-processor (that post-processor
     * corrupts the shared {@code CsrfTokenRepository} at the ServletContext level for
     * every other test in this Surefire fork — see
     * {@code com.tuhospedaje.AbstractIntegrationTest#obtainCsrfCookie}). This class does
     * not extend {@code AbstractIntegrationTest} (it manages its own cleanup via
     * {@code @AfterEach} instead of {@code @Transactional} rollback), so the same logic
     * is duplicated here rather than changing this file's test-isolation strategy.
     */
    private Cookie obtainCsrfCookie() throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/api/policies"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        return csrfCookie;
    }

    /**
     * Builds the {@code ACCESS_TOKEN} cookie used to authenticate {@code MockMvc}
     * requests. Duplicated here rather than via {@code AbstractIntegrationTest} for the
     * same reason {@link #obtainCsrfCookie()} is duplicated above — this class does not
     * extend that base class.
     */
    private Cookie accessCookie(String token) {
        return new Cookie("ACCESS_TOKEN", token);
    }
}
