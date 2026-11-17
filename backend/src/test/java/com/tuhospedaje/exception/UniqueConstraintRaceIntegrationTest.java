package com.tuhospedaje.exception;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.category.CategoryDTO;
import com.tuhospedaje.dto.features.FeatureDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deterministic HTTP-level regression tests for issue #455: when two concurrent requests
 * pass the service's duplicate pre-check and the second hits MariaDB's unique constraint,
 * the API must answer 409 Conflict with the same localized payload as the sequential path
 * instead of 400 error.data_integrity.
 *
 * <p>Does NOT extend {@link AbstractIntegrationTest} because class-level {@code @Transactional}
 * would share a single Hibernate session with the controller/service call and mask real
 * constraint-flush rollbacks across multiple requests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UniqueConstraintRaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @MockitoSpyBean
    private CategoryRepository categoryRepository;

    @MockitoSpyBean
    private FeatureRepository featureRepository;

    @MockitoSpyBean
    private LodgingRepository lodgingRepository;

    @MockitoSpyBean
    private UserRepository userRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        User admin = User.builder()
                .firstName("Admin")
                .lastName("Race")
                .email("admin-race-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build();
        User savedAdmin = userRepository.save(admin);
        adminToken = jwtService.generateToken(savedAdmin);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        AbstractIntegrationTest.clearForeignKeyChildrenOfUsers(jdbcTemplate);
        lodgingRepository.deleteAll();
        categoryRepository.deleteAll();
        featureRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturn409WhenCreateCategoryHitsUniqueNameConstraint() throws Exception {
        Category existing = new Category();
        existing.setName("Cabana Race");
        existing.setDescription("Inicial");
        existing.setImageUrl("https://cdn.tuhospedaje.test/categories/cabana.jpg");
        categoryRepository.saveAndFlush(existing);

        doReturn(false).when(categoryRepository).existsByName("Cabana Race");

        CategoryDTO request = new CategoryDTO();
        request.setName("Cabana Race");
        request.setDescription("Concurrente");
        request.setImageUrl("https://cdn.tuhospedaje.test/categories/cabana2.jpg");

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(post("/api/categories")
                        .locale(Locale.forLanguageTag("es"))
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Ya existe una categoría con el nombre: Cabana Race"));

        mockMvc.perform(post("/api/categories")
                        .locale(Locale.ENGLISH)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("A category with the name Cabana Race already exists."));

        assertThat(categoryRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturn409WhenUpdateCategoryHitsUniqueNameConstraint() throws Exception {
        Category first = new Category();
        first.setName("Hotel Race");
        first.setDescription("Inicial");
        first.setImageUrl("https://cdn.tuhospedaje.test/categories/hotel.jpg");
        Category savedFirst = categoryRepository.saveAndFlush(first);

        Category second = new Category();
        second.setName("Hostel Race");
        second.setDescription("Segundo");
        second.setImageUrl("https://cdn.tuhospedaje.test/categories/hostel.jpg");
        categoryRepository.saveAndFlush(second);

        doReturn(Optional.empty()).when(categoryRepository).findByNameIgnoreCase("Hostel Race");

        CategoryDTO request = new CategoryDTO();
        request.setName("Hostel Race");
        request.setDescription("Renombrada");

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(put("/api/categories/{id}", savedFirst.getId())
                        .locale(Locale.forLanguageTag("es"))
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Ya existe una categoría con el nombre: Hostel Race"));

        mockMvc.perform(put("/api/categories/{id}", savedFirst.getId())
                        .locale(Locale.ENGLISH)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("A category with the name Hostel Race already exists."));

        assertThat(categoryRepository.findById(savedFirst.getId()))
                .hasValueSatisfying(cat -> assertThat(cat.getName()).isEqualTo("Hotel Race"));
    }

    @Test
    void shouldReturn409WhenCreateFeatureHitsUniqueNameConstraint() throws Exception {
        Feature existing = new Feature();
        existing.setName("FT-Race-WiFi");
        existing.setIcon("wifi-icon");
        featureRepository.saveAndFlush(existing);

        doReturn(false).when(featureRepository).existsByNameIgnoreCase("FT-Race-WiFi");

        FeatureDTO request = new FeatureDTO();
        request.setName("FT-Race-WiFi");
        request.setIcon("wifi-icon");

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(post("/api/features")
                        .locale(Locale.forLanguageTag("es"))
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Ya existe una característica con el nombre: FT-Race-WiFi"));

        mockMvc.perform(post("/api/features")
                        .locale(Locale.ENGLISH)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("A feature with the name FT-Race-WiFi already exists."));

        assertThat(featureRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturn409WhenUpdateFeatureHitsUniqueNameConstraint() throws Exception {
        Feature first = new Feature();
        first.setName("FT-Race-A");
        first.setIcon("icon-a");
        Feature savedFirst = featureRepository.saveAndFlush(first);

        Feature second = new Feature();
        second.setName("FT-Race-B");
        second.setIcon("icon-b");
        featureRepository.saveAndFlush(second);

        doReturn(null).when(featureRepository).findByNameIgnoreCase("FT-Race-B");

        FeatureDTO request = new FeatureDTO();
        request.setName("FT-Race-B");
        request.setIcon("icon-a");

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(put("/api/features/{id}", savedFirst.getId())
                        .locale(Locale.forLanguageTag("es"))
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Ya existe una característica con el nombre: FT-Race-B"));

        mockMvc.perform(put("/api/features/{id}", savedFirst.getId())
                        .locale(Locale.ENGLISH)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("A feature with the name FT-Race-B already exists."));

        assertThat(featureRepository.findById(savedFirst.getId()))
                .hasValueSatisfying(feat -> assertThat(feat.getName()).isEqualTo("FT-Race-A"));
    }

    @Test
    void shouldReturn409WhenCreateLodgingHitsUniqueNameConstraint() throws Exception {
        Lodging existing = buildLodging("Hotel Race Unico", "race-uno@tuhospedaje.com");
        lodgingRepository.saveAndFlush(existing);

        doReturn(false).when(lodgingRepository).existsByName("Hotel Race Unico");

        Map<String, Object> request = lodgingPayload("Hotel Race Unico", "race-dos@tuhospedaje.com", null);

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(post("/api/lodgings")
                        .locale(Locale.forLanguageTag("es"))
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Ya existe un alojamiento con el nombre: Hotel Race Unico"));

        mockMvc.perform(post("/api/lodgings")
                        .locale(Locale.ENGLISH)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("A lodging with the name Hotel Race Unico already exists."));

        assertThat(lodgingRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturn409WhenCreateLodgingHitsUniqueEmailConstraint() throws Exception {
        Lodging existing = buildLodging("Hotel Race Alpha", "race-dup@tuhospedaje.com");
        lodgingRepository.saveAndFlush(existing);

        doReturn(false).when(lodgingRepository).existsByEmail("race-dup@tuhospedaje.com");

        Map<String, Object> request = lodgingPayload("Hotel Race Beta", "race-dup@tuhospedaje.com", null);

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(post("/api/lodgings")
                        .locale(Locale.forLanguageTag("es"))
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Ya existe un alojamiento con el email: race-dup@tuhospedaje.com"));

        mockMvc.perform(post("/api/lodgings")
                        .locale(Locale.ENGLISH)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("A lodging with the email race-dup@tuhospedaje.com already exists."));

        assertThat(lodgingRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturn409WhenUpdateLodgingHitsUniqueNameConstraint() throws Exception {
        Lodging first = lodgingRepository.saveAndFlush(buildLodging("Hotel Race Uno", "uno@tuhospedaje.com"));
        lodgingRepository.saveAndFlush(buildLodging("Hotel Race Dos", "dos@tuhospedaje.com"));

        doReturn(false).when(lodgingRepository).existsByName("Hotel Race Dos");

        Map<String, Object> request = lodgingPayload("Hotel Race Dos", "uno@tuhospedaje.com", first.getVersion());

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(put("/api/lodgings/{id}", first.getId())
                        .locale(Locale.forLanguageTag("es"))
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Ya existe un alojamiento con el nombre: Hotel Race Dos"));

        mockMvc.perform(put("/api/lodgings/{id}", first.getId())
                        .locale(Locale.ENGLISH)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("A lodging with the name Hotel Race Dos already exists."));

        assertThat(lodgingRepository.findById(first.getId()))
                .hasValueSatisfying(lodging -> assertThat(lodging.getName()).isEqualTo("Hotel Race Uno"));
    }

    @Test
    void shouldReturn409WhenUpdateLodgingHitsUniqueEmailConstraint() throws Exception {
        Lodging first = lodgingRepository.saveAndFlush(buildLodging("Hotel Race Uno", "uno@tuhospedaje.com"));
        lodgingRepository.saveAndFlush(buildLodging("Hotel Race Dos", "dos@tuhospedaje.com"));

        doReturn(false).when(lodgingRepository).existsByEmail("dos@tuhospedaje.com");

        Map<String, Object> request = lodgingPayload("Hotel Race Uno", "dos@tuhospedaje.com", first.getVersion());

        Cookie csrfCookie = obtainCsrfCookie();
        mockMvc.perform(put("/api/lodgings/{id}", first.getId())
                        .locale(Locale.forLanguageTag("es"))
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Ya existe un alojamiento con el email: dos@tuhospedaje.com"));

        mockMvc.perform(put("/api/lodgings/{id}", first.getId())
                        .locale(Locale.ENGLISH)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("A lodging with the email dos@tuhospedaje.com already exists."));

        assertThat(lodgingRepository.findById(first.getId()))
                .hasValueSatisfying(lodging -> assertThat(lodging.getEmail()).isEqualTo("uno@tuhospedaje.com"));
    }

    @Test
    void shouldReturn409WithDuplicateEmailCodeWhenRegisterHitsUniqueEmailConstraint() throws Exception {
        User existing = User.builder()
                .firstName("Juan")
                .lastName("Perez")
                .email("race-auth@tuhospedaje.com")
                .password("Pass1234")
                .role(RoleEnum.USER)
                .build();
        userRepository.saveAndFlush(existing);

        doReturn(Optional.empty()).when(userRepository).findByEmail("race-auth@tuhospedaje.com");

        RegisterRequest request = new RegisterRequest("Otro", "Nombre", "race-auth@tuhospedaje.com", "Pass1234");
        Cookie csrfCookie = obtainCsrfCookie();

        mockMvc.perform(post("/api/auth/register")
                        .locale(Locale.forLanguageTag("es"))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("duplicate_email"))
                .andExpect(jsonPath("$.error").value("Ese email ya está registrado."));

        mockMvc.perform(post("/api/auth/register")
                        .locale(Locale.ENGLISH)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("duplicate_email"))
                .andExpect(jsonPath("$.error").value("This email is already registered."));

        Optional<User> stored = jdbcTemplate.query(
                "SELECT id, first_name FROM users WHERE email = ?",
                (rs, rowNum) -> {
                    User u = new User();
                    u.setId(rs.getLong("id"));
                    u.setFirstName(rs.getString("first_name"));
                    return u;
                },
                "race-auth@tuhospedaje.com"
        ).stream().findFirst();
        assertThat(stored).hasValueSatisfying(user -> assertThat(user.getFirstName()).isEqualTo("Juan"));
    }

    private Lodging buildLodging(String name, String email) {
        Lodging lodging = new Lodging();
        lodging.setName(name);
        lodging.setDescription("Descripción");
        lodging.setAddress("Calle 123");
        lodging.setCity("Ciudad");
        lodging.setCountry("País");
        lodging.setPhoneNumber("123456789");
        lodging.setEmail(email);
        lodging.setPricePerNight(new BigDecimal("35000.75"));
        lodging.setMaxGuests(5);
        return lodging;
    }

    private Map<String, Object> lodgingPayload(String name, String email, Long version) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", "Descripción");
        payload.put("address", "Calle 123");
        payload.put("city", "Ciudad");
        payload.put("country", "País");
        payload.put("phoneNumber", "123456789");
        payload.put("email", email);
        payload.put("pricePerNight", new BigDecimal("35000.75"));
        payload.put("maxGuests", 5);
        if (version != null) {
            payload.put("version", version);
        }
        return payload;
    }

    private Cookie obtainCsrfCookie() throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/api/categories"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        return csrfCookie;
    }

    private Cookie accessCookie(String token) {
        return new Cookie("ACCESS_TOKEN", token);
    }
}
