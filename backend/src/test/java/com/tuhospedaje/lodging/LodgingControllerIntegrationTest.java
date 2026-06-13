package com.tuhospedaje.lodging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.ReservationRepository;
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
import java.time.LocalDate;
import java.util.Map;

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
    private ReservationRepository reservationRepository;

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
    void shouldReturnBadRequestWhenCreatingLodgingWithInvalidPayload() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "",
                "address", "Calle 123",
                "city", "Ciudad",
                "country", "País",
                "phoneNumber", "123456789",
                "email", "invalid-email"
        );

        mockMvc.perform(post("/api/lodgings")
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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

        mockMvc.perform(post("/api/lodgings")
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader)
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

        mockMvc.perform(post("/api/lodgings")
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
    void shouldSearchLodgingsByCity() throws Exception {
        createTestLodging("Hotel Boutique", "boutique@test.com");

        mockMvc.perform(get("/api/lodgings/search")
                        .param("city", "Ciudad"))
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
                .andExpect(jsonPath("$[*].id", hasItem(id.intValue())));
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
                .andExpect(jsonPath("$[*].id", not(hasItem(id.intValue()))));
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
                .andExpect(jsonPath("$[*].id", hasItem(id.intValue())));
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
                .andExpect(jsonPath("$[*].id", hasItem(id.intValue())));
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
