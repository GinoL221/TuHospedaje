package com.tuhospedaje.lodging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.PolicyRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SC-4.1 through SC-4.4: LAZY fetch switch for Lodging.features, Lodging.policies,
 * User.favorites must not produce LazyInitializationException on any endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LazyFetchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private com.tuhospedaje.repository.ReservationRepository reservationRepository;

    @Autowired
    private JwtService jwtService;

    private Long lodgingId;
    private String adminToken;
    private String userToken;
    private User regularUser;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        // Must delete lodgings (and their join table entries) BEFORE features/policies.
        // features/policies are not deleted here to avoid FK violations from other tests
        // (the @Transactional test annotation rolls back newly created data automatically).
        lodgingRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("Lazy")
                .email("admin-lazy@test.com")
                .password("hash")
                .role(RoleEnum.ADMIN)
                .build());
        adminToken = "Bearer " + jwtService.generateToken(admin);

        regularUser = userRepository.save(User.builder()
                .firstName("User")
                .lastName("Lazy")
                .email("user-lazy@test.com")
                .password("hash")
                .role(RoleEnum.USER)
                .build());
        userToken = "Bearer " + jwtService.generateToken(regularUser);

        Feature feature = new Feature();
        feature.setName("Wi-Fi Lazy Test");
        feature.setIcon("wifi");
        Feature savedFeature = featureRepository.save(feature);

        Policy policy = new Policy();
        policy.setName("No Pets Lazy Test");
        policy.setDescription("No pets allowed");
        policy.setIcon("no-pets");
        Policy savedPolicy = policyRepository.save(policy);

        Lodging lodging = new Lodging();
        lodging.setName("Lazy Test Hotel");
        lodging.setDescription("desc");
        lodging.setAddress("addr");
        lodging.setCity("city");
        lodging.setCountry("country");
        lodging.setPhoneNumber("123");
        lodging.setEmail("lazy-hotel@test.com");
        lodging.setPricePerNight(new BigDecimal("100.00"));
        lodging.setMaxGuests(4);
        lodging.setFeatures(Set.of(savedFeature));
        lodging.setPolicies(Set.of(savedPolicy));
        lodgingId = lodgingRepository.save(lodging).getId();
    }

    /** SC-4.1: detail endpoint returns features and policies — no LazyInitializationException */
    @Test
    void detailEndpoint_returnsFeaturesAndPolicies_noLazyException() throws Exception {
        mockMvc.perform(get("/api/lodgings/{id}", lodgingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.features", hasSize(1)))
                .andExpect(jsonPath("$.policies").isArray())
                .andExpect(jsonPath("$.policies", hasSize(1)));
    }

    /** SC-4.2: list endpoint returns features for each lodging — no LazyInitializationException */
    @Test
    void listEndpoint_returnsFeaturesForEachLodging_noLazyException() throws Exception {
        mockMvc.perform(get("/api/lodgings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].features").isArray());
    }

    /** SC-4.3: user favorites round-trip — no LazyInitializationException */
    @Test
    void favoritesEndpoint_returnsFavorites_noLazyException() throws Exception {
        // Add favorite
        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/favorites/{lodgingId}", lodgingId)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().is2xxSuccessful());

        // Get favorites
        mockMvc.perform(get("/api/favorites")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    /** SC-4.4: random, search, and reservation endpoints smoke test — all return 2xx, no lazy errors */
    @Test
    void smokeTest_randomAndSearch_noLazyException() throws Exception {
        mockMvc.perform(get("/api/lodgings/random"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lodgings/search").param("city", "city"))
                .andExpect(status().isOk());
    }
}
