package com.tuhospedaje.lodging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
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
    private CategoryRepository categoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManagerFactory emf;

    private String adminToken;
    private String userToken;

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
        adminToken = jwtService.generateToken(savedAdmin);

        User regularUser = User.builder()
                .firstName("User")
                .lastName("Lodging")
                .email("user-lodging-crud-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build();
        User savedUser = userRepository.save(regularUser);
        userToken = jwtService.generateToken(savedUser);
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
                "email", "hotel-test@tuhospedaje.com",
                "pricePerNight", new BigDecimal("35000.75"),
                "maxGuests", 5
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Hotel Test"))
                .andExpect(jsonPath("$.pricePerNight").value(35000.75))
                .andExpect(jsonPath("$.maxGuests").value(5));
    }

    @Test
    void shouldReturnBadRequestWhenCreatingLodgingWithInvalidPayload() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "",
                "address", "Calle 123",
                "city", "Ciudad",
                "country", "País",
                "phoneNumber", "123456789",
                "email", "invalid-email"
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnValidationFieldsWhenPriceAndCapacityAreMissing() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "Hotel sin tarifa",
                "address", "Calle 123",
                "city", "Ciudad",
                "country", "País",
                "phoneNumber", "123456789",
                "email", "missing-price@test.com"
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.pricePerNight").exists())
                .andExpect(jsonPath("$.fields.maxGuests").exists());
    }

    @Test
    void shouldReturnBadRequestWhenCreatingLodgingWithNonNullId() throws Exception {
        Map<String, Object> request = Map.of(
                "id", 123L,
                "name", "Hotel ID Test",
                "address", "Calle 123",
                "city", "Ciudad",
                "country", "País",
                "phoneNumber", "123456789",
                "email", "id-test@tuhospedaje.com"
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnForbiddenWhenCreatingLodgingWithoutAuth() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "Sin Auth",
                "email", "noauth@test.com"
        );

        // Keep CSRF valid even without auth, so the 403 is attributable to the missing
        // token, not to a missing CSRF header (design's explicit ordering-trap warning).
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/lodgings")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenCreatingLodgingWithUserRole() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "User Role Hotel",
                "description", "Descripción",
                "address", "Calle 123",
                "city", "Ciudad",
                "country", "País",
                "phoneNumber", "123456789",
                "email", "userrole@test.com",
                "pricePerNight", new BigDecimal("30000.00"),
                "maxGuests", 4
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(userToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
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
    void adminLodgings_returnsServerPaginatedSearchResults() throws Exception {
        createTestLodgingWithCity("Admin Page Alpha", "admin-page-alpha@tdd.com", "tdd-admin-page");
        createTestLodgingWithCity("Admin Page Beta", "admin-page-beta@tdd.com", "tdd-admin-page");
        createTestLodgingWithCity("Admin Page Gamma", "admin-page-gamma@tdd.com", "tdd-admin-page");

        mockMvc.perform(get("/api/lodgings/admin")
                        .cookie(accessCookie(adminToken))
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name")
                        .param("direction", "asc")
                        .param("q", "tdd-admin-page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Admin Page Alpha"))
                .andExpect(jsonPath("$.items[1].name").value("Admin Page Beta"))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void adminLodgings_forbidsAuthenticatedNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/lodgings/admin")
                        .cookie(accessCookie(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminLodgings_rejectsOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/lodgings/admin")
                        .cookie(accessCookie(adminToken))
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminLodgings_rejectsUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/lodgings/admin")
                        .cookie(accessCookie(adminToken))
                        .param("sort", "notARealField"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminLodgings_rejectsUnknownSortDirection() throws Exception {
        mockMvc.perform(get("/api/lodgings/admin")
                        .cookie(accessCookie(adminToken))
                        .param("direction", "sideways"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSearchLodgingsByCity() throws Exception {
        createTestLodging("Hotel Boutique", "boutique@test.com");

        mockMvc.perform(get("/api/lodgings/search")
                        .param("city", "Ciudad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings[0].name").value("Hotel Boutique"))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalItems").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void shouldSearchLodgingsWithDefaultPagination() throws Exception {
        createTestLodgingWithCity("Hotel Default Page", "default-page@tdd-pag-01.com", "tdd-pag-01");

        mockMvc.perform(get("/api/lodgings/search")
                        .param("city", "tdd-pag-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.lodgings[0].name").value("Hotel Default Page"));
    }

    @Test
    void shouldFilterSearchByMultipleCategories() throws Exception {
        Long categoryHotelId = createTestCategory("Hotel Multi-Cat A");
        Long categoryHostelId = createTestCategory("Hostel Multi-Cat B");

        Long hotelId = createTestLodgingWithCategory("Hotel In Categories", "in-cat@tdd-multicat-01.com", categoryHotelId);
        Long hostelId = createTestLodgingWithCategory("Hostel In Categories", "in-cat-2@tdd-multicat-01.com", categoryHostelId);
        createTestLodging("Lodging Without Category", "no-cat@tdd-multicat-01.com");

        mockMvc.perform(get("/api/lodgings/search")
                        .param("categories", categoryHotelId + "," + categoryHostelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings[*].id", hasItems(hotelId.intValue(), hostelId.intValue())));
    }

    @Test
    void shouldReturnEmptyLodgingsWhenSearchPageIsOutOfBounds() throws Exception {
        createTestLodgingWithCity("Hotel Out Of Bounds", "oob@tdd-oob-01.com", "tdd-oob-01");

        mockMvc.perform(get("/api/lodgings/search")
                        .param("city", "tdd-oob-01")
                        .param("page", "999")
                        .param("size", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings").isArray())
                .andExpect(jsonPath("$.lodgings").isEmpty())
                .andExpect(jsonPath("$.currentPage").value(999));
    }

    @Test
    void shouldReturnBadRequestWhenSearchPageIsNegative() throws Exception {
        mockMvc.perform(get("/api/lodgings/search")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSearchSizeIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/lodgings/search")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnSpanishMessageWhenSearchPageIsNegativeAndAcceptLanguageIsEs() throws Exception {
        mockMvc.perform(get("/api/lodgings/search")
                        .param("page", "-1")
                        .header("Accept-Language", "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El índice de página no debe ser negativo."));
    }

    @Test
    void shouldReturnEnglishMessageWhenSearchPageIsNegativeAndAcceptLanguageIsMissing() throws Exception {
        mockMvc.perform(get("/api/lodgings/search")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Page index must not be negative."));
    }

    @Test
    void shouldReturnEnglishMessageWhenSearchSizeIsNotPositiveAndAcceptLanguageIsEn() throws Exception {
        mockMvc.perform(get("/api/lodgings/search")
                        .param("size", "0")
                        .header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Size must be greater than zero."));
    }

    @Test
    void shouldReturnSpanishMessageWhenSearchSizeIsNotPositiveAndAcceptLanguageIsEs() throws Exception {
        mockMvc.perform(get("/api/lodgings/search")
                        .param("size", "0")
                        .header("Accept-Language", "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El tamaño debe ser mayor a cero."));
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
                "email", "update@test.com",
                "pricePerNight", new BigDecimal("42000.50"),
                "maxGuests", 8
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(put("/api/lodgings/{id}", id)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Actualizado"))
                .andExpect(jsonPath("$.description").value("Nueva descripción"))
                .andExpect(jsonPath("$.pricePerNight").value(42000.50))
                .andExpect(jsonPath("$.maxGuests").value(8));

        Lodging updated = lodgingRepository.findById(id).orElseThrow();
        assertThat(updated.getPricePerNight()).isEqualByComparingTo("42000.50");
        assertThat(updated.getMaxGuests()).isEqualTo(8);
    }

    @Test
    void shouldPersistOrderedImagesWhenCreatingAndUpdatingLodging() throws Exception {
        Map<String, Object> createRequest = Map.of(
                "name", "Image order",
                "description", "Initial images",
                "address", "Calle 123",
                "city", "Ciudad",
                "country", "País",
                "phoneNumber", "123456789",
                "email", "image-order@test.com",
                "pricePerNight", new BigDecimal("30000.00"),
                "maxGuests", 4,
                "imageUrls", java.util.List.of("https://example.com/one.jpg", "https://example.com/two.jpg")
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        String createResponse = mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrls[0]").value("https://example.com/one.jpg"))
                .andExpect(jsonPath("$.imageUrls[1]").value("https://example.com/two.jpg"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        Map<String, Object> updateRequest = new java.util.HashMap<>(createRequest);
        updateRequest.put("name", "Updated image order");
        updateRequest.put("imageUrls", java.util.List.of("https://example.com/three.jpg", "https://example.com/four.jpg"));

        csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(put("/api/lodgings/{id}", id)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrls[0]").value("https://example.com/three.jpg"))
                .andExpect(jsonPath("$.imageUrls[1]").value("https://example.com/four.jpg"));

        mockMvc.perform(get("/api/lodgings/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrls[0]").value("https://example.com/three.jpg"))
                .andExpect(jsonPath("$.imageUrls[1]").value("https://example.com/four.jpg"))
                .andExpect(jsonPath("$.imageUrls.length()").value(2));
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingLodgingWithoutAuth() throws Exception {
        // Keep CSRF valid even without auth, so the 403 is attributable to the missing
        // token, not to a missing CSRF header (design's explicit ordering-trap warning).
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(put("/api/lodgings/{id}", 1L)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hack"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenDeletingLodgingWithoutAuth() throws Exception {
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(delete("/api/lodgings/{id}", 1L)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteLodgingSuccessfully() throws Exception {
        Long id = createTestLodging("To Delete", "delete@test.com");

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(delete("/api/lodgings/{id}", id)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());

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
                "email", email,
                "pricePerNight", new BigDecimal("30000.00"),
                "maxGuests", 4
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        String response = mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createTestCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(name + " description");
        return categoryRepository.save(category).getId();
    }

    private Long createTestLodgingWithCategory(String name, String email, Long categoryId) throws Exception {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("name", name);
        request.put("description", "Descripción");
        request.put("address", "Calle 123");
        request.put("city", "Ciudad");
        request.put("country", "País");
        request.put("phoneNumber", "123456789");
        request.put("email", email);
        request.put("categoryId", categoryId);
        request.put("pricePerNight", new BigDecimal("30000.00"));
        request.put("maxGuests", 4);

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        String response = mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void shouldReturnDistinctCities() throws Exception {
        createTestLodgingWithCity("Hotel A", "a@testcities.com", "Springfield");
        createTestLodgingWithCity("Hotel B", "b@testcities.com", "Springfield");
        createTestLodgingWithCity("Hotel C", "c@testcities.com", "Boston");
        createTestLodgingWithCity("Hotel D", "d@testcities.com", "New York");

        mockMvc.perform(get("/api/lodgings/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasItems("Boston", "New York", "Springfield")));

        mockMvc.perform(get("/api/lodgings/cities").param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("Springfield"));
    }

    private Long createTestLodgingWithCity(String name, String email, String city) throws Exception {
        Map<String, Object> request = Map.of(
                "name", name,
                "description", "Descripción",
                "address", "Calle 123",
                "city", city,
                "country", "País",
                "phoneNumber", "123456789",
                "email", email,
                "pricePerNight", new BigDecimal("30000.00"),
                "maxGuests", 4
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        String response = mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    // -------------------------------------------------------------------------
    // Availability filtering tests (N+1 fix validation)
    // -------------------------------------------------------------------------

    @Test
    void availableLodging_noReservations_appearsInResults() throws Exception {
        Long id = createTestLodgingWithCity("Avail No Res", "avail-no-res@tdd-avail-01.com", "tdd-avail-01");
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/lodgings/search")
                        .param("city", "tdd-avail-01")
                        .param("checkIn", today.plusDays(1).toString())
                        .param("checkOut", today.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings[*].id", hasItem(id.intValue())));
    }

    @Test
    void confirmedOverlappingReservation_lodgingExcluded() throws Exception {
        Long id = createTestLodgingWithCity("Booked Hotel", "booked@tdd-excl-02.com", "tdd-excl-02");
        LocalDate today = LocalDate.now();
        seedReservation(id, today, today.plusDays(5), ReservationStatus.CONFIRMED);

        mockMvc.perform(get("/api/lodgings/search")
                        .param("city", "tdd-excl-02")
                        .param("checkIn", today.plusDays(1).toString())
                        .param("checkOut", today.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings[*].id", not(hasItem(id.intValue()))));
    }

    @Test
    void adjacentReservation_checkoutEqualsRequestedCheckin_lodgingIncluded() throws Exception {
        Long id = createTestLodgingWithCity("Adjacent Hotel", "adjacent@tdd-adj-03.com", "tdd-adj-03");
        LocalDate today = LocalDate.now();
        // Reservation checkOut == requested checkIn: adjacent, NOT an overlap
        seedReservation(id, today.minusDays(2), today.plusDays(1), ReservationStatus.CONFIRMED);

        mockMvc.perform(get("/api/lodgings/search")
                        .param("city", "tdd-adj-03")
                        .param("checkIn", today.plusDays(1).toString())
                        .param("checkOut", today.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings[*].id", hasItem(id.intValue())));
    }

    @Test
    void cancelledOverlappingReservation_doesNotBlockLodging() throws Exception {
        Long id = createTestLodgingWithCity("Cancelled Hotel", "cancelled@tdd-canc-04.com", "tdd-canc-04");
        LocalDate today = LocalDate.now();
        // CANCELLED reservation overlapping the requested range — must NOT block
        seedReservation(id, today, today.plusDays(5), ReservationStatus.CANCELLED);

        mockMvc.perform(get("/api/lodgings/search")
                        .param("city", "tdd-canc-04")
                        .param("checkIn", today.plusDays(1).toString())
                        .param("checkOut", today.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings[*].id", hasItem(id.intValue())));
    }

    @Test
    void adjacentReservation_checkinEqualsRequestedCheckout_lodgingIncluded() throws Exception {
        Long id = createTestLodgingWithCity("Adjacent Hotel B", "adjacent-b@tdd-adj-05.com", "tdd-adj-05");
        LocalDate today = LocalDate.now();
        // Reservation checkIn == requested checkOut: adjacent, NOT an overlap
        seedReservation(id, today.plusDays(3), today.plusDays(7), ReservationStatus.CONFIRMED);

        mockMvc.perform(get("/api/lodgings/search")
                        .param("city", "tdd-adj-05")
                        .param("checkIn", today.plusDays(1).toString())
                        .param("checkOut", today.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings[*].id", hasItem(id.intValue())));
    }

    @Test
    void availabilityEndpoint_returnsOccupiedRangesAndUnavailableForOverlappingRequestedRange() throws Exception {
        Long id = createTestLodgingWithCity("Availability Hotel", "availability@tdd-endpoint-01.com", "tdd-endpoint-01");
        LocalDate today = LocalDate.now();
        seedReservation(id, today.plusDays(2), today.plusDays(5), ReservationStatus.CONFIRMED);
        seedReservation(id, today.plusDays(8), today.plusDays(10), ReservationStatus.CANCELLED);

        mockMvc.perform(get("/api/lodgings/{id}/availability", id)
                        .param("checkIn", today.plusDays(3).toString())
                        .param("checkOut", today.plusDays(4).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.occupiedRanges.length()").value(1))
                .andExpect(jsonPath("$.occupiedRanges[0].checkIn").value(today.plusDays(2).toString()))
                .andExpect(jsonPath("$.occupiedRanges[0].checkOut").value(today.plusDays(5).toString()));
    }

    @Test
    void availabilityEndpoint_treatsAdjacentRequestedRangeAsAvailable() throws Exception {
        Long id = createTestLodgingWithCity("Adjacent Availability Hotel", "availability@tdd-endpoint-02.com", "tdd-endpoint-02");
        LocalDate today = LocalDate.now();
        seedReservation(id, today.plusDays(2), today.plusDays(5), ReservationStatus.CONFIRMED);

        mockMvc.perform(get("/api/lodgings/{id}/availability", id)
                        .param("checkIn", today.plusDays(5).toString())
                        .param("checkOut", today.plusDays(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.occupiedRanges.length()").value(1));
    }

    @Test
    void searchWithDates_executesAtMostTwoQueries() throws Exception {
        Long id = createTestLodgingWithCity("Perf Hotel", "perf@tdd-perf-06.com", "tdd-perf-06");
        LocalDate today = LocalDate.now();

        SessionFactory sf = emf.unwrap(SessionFactory.class);
        sf.getStatistics().setStatisticsEnabled(true);
        sf.getStatistics().clear();

        mockMvc.perform(get("/api/lodgings/search")
                        .param("city", "tdd-perf-06")
                        .param("checkIn", today.plusDays(1).toString())
                        .param("checkOut", today.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings[*].id", hasItem(id.intValue())));

        // search (1 query with NOT EXISTS subquery) + ratings aggregate (1 query) = 2 max
        long queryCount = sf.getStatistics().getQueryExecutionCount();
        assertThat(queryCount).isLessThanOrEqualTo(2L);
    }

    private void seedReservation(Long lodgingId, LocalDate checkIn, LocalDate checkOut, ReservationStatus status) {
        Lodging lodging = lodgingRepository.findById(lodgingId)
                .orElseThrow(() -> new IllegalArgumentException("Lodging not found: " + lodgingId));

        User guest = User.builder()
                .firstName("Guest")
                .lastName("Tester")
                .email("guest-" + lodgingId + "-" + checkIn + "@reservation-seed.com")
                .password("password")
                .role(RoleEnum.USER)
                .build();
        User savedGuest = userRepository.save(guest);

        Reservation reservation = new Reservation();
        reservation.setLodging(lodging);
        reservation.setUser(savedGuest);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuestName(savedGuest.getFirstName() + " " + savedGuest.getLastName());
        reservation.setGuestEmail(savedGuest.getEmail());
        reservation.setGuestPhone("000000000");
        reservation.setTotalPrice(BigDecimal.valueOf(100));
        reservation.setStatus(status);
        reservationRepository.save(reservation);
    }
}
