package com.tuhospedaje.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.EmailService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EmailService emailService;

    private String userAuthHeader;

    @BeforeEach
    void setUp() {
        // Seeded ratings reference both lodgings and users, so they must go first
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        lodgingRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .firstName("Juan")
                .lastName("Perez")
                .email("juan-reservas@test.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build();

        User savedUser = userRepository.save(user);
        userAuthHeader = jwtService.generateToken(savedUser);
    }

    @Test
    void shouldCreateReservationSuccessfullyAndSendConfirmationEmail() throws Exception {
        Long lodgingId = createTestLodging();

        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = LocalDate.now().plusDays(12);

        Map<String, Object> request = Map.of(
                "lodgingId", lodgingId,
                "checkIn", checkIn.toString(),
                "checkOut", checkOut.toString(),
                "guestName", "Juan Perez",
                "guestEmail", "juan-reservas@test.com",
                "guestPhone", "+5491122334455"
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/reservations")
                        .cookie(accessCookie(userAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.guestName").value("Juan Perez"))
                .andExpect(jsonPath("$.guestEmail").value("juan-reservas@test.com"))
                .andExpect(jsonPath("$.guestPhone").value("+5491122334455"));

        verify(emailService, times(1)).sendReservationConfirmation(any(ReservationResponse.class));
    }

    @Test
    void shouldReturnUserReservationsOrderedByCheckInDesc() throws Exception {
        Long lodgingId = createTestLodging();

        createReservation(lodgingId, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));
        createReservation(lodgingId, LocalDate.now().plusDays(20), LocalDate.now().plusDays(22));

        mockMvc.perform(get("/api/reservations/my")
                        .cookie(accessCookie(userAuthHeader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].checkIn").value(LocalDate.now().plusDays(20).toString()))
                .andExpect(jsonPath("$[1].checkIn").value(LocalDate.now().plusDays(10).toString()));
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatingReservationWithoutAuth() throws Exception {
        Long lodgingId = createTestLodging();

        Map<String, Object> request = Map.of(
                "lodgingId", lodgingId,
                "checkIn", LocalDate.now().plusDays(10).toString(),
                "checkOut", LocalDate.now().plusDays(12).toString(),
                "guestName", "Juan Perez",
                "guestEmail", "juan@test.com",
                "guestPhone", "+5491122334455"
        );

        // Keep CSRF valid even without auth, so the 401 is attributable to the missing
        // token, not to a missing CSRF header (design's explicit ordering-trap warning).
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/reservations")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedWhenGettingHistoryWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/reservations/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnAllReservationsForAdminOrderedByIdDesc() throws Exception {
        User admin = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin-reservas@test.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build();
        User savedAdmin = userRepository.save(admin);
        String adminAuth = jwtService.generateToken(savedAdmin);

        Long lodgingId = createTestLodging();

        createReservation(lodgingId, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));
        createReservation(lodgingId, LocalDate.now().plusDays(20), LocalDate.now().plusDays(22));

        mockMvc.perform(get("/api/reservations")
                        .cookie(accessCookie(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].checkIn").value(LocalDate.now().plusDays(20).toString()))
                .andExpect(jsonPath("$[1].checkIn").value(LocalDate.now().plusDays(10).toString()));
    }

    @Test
    void shouldReturnForbiddenWhenGettingAllReservationsAsNormalUser() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .cookie(accessCookie(userAuthHeader)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnUnauthorizedWhenGettingAllReservationsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    private Long createTestLodging() {
        Lodging lodging = new Lodging();
        lodging.setName("Hotel Test");
        lodging.setDescription("Descripcion");
        lodging.setAddress("Calle 123");
        lodging.setCity("Ciudad");
        lodging.setCountry("Pais");
        lodging.setPhoneNumber("123456789");
        lodging.setEmail("hotel-test@test.com");
        lodging.setPricePerNight(new BigDecimal("100.00"));
        lodging.setMaxGuests(4);

        return lodgingRepository.save(lodging).getId();
    }

    private void createReservation(Long lodgingId, LocalDate checkIn, LocalDate checkOut) throws Exception {
        Map<String, Object> request = Map.of(
                "lodgingId", lodgingId,
                "checkIn", checkIn.toString(),
                "checkOut", checkOut.toString(),
                "guestName", "Juan Perez",
                "guestEmail", "juan-reservas@test.com",
                "guestPhone", "+5491122334455"
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/reservations")
                        .cookie(accessCookie(userAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnOnlyAuthenticatedUserOwnReservations() throws Exception {
        User otherUser = User.builder()
                .firstName("Other")
                .lastName("User")
                .email("other-isolation@test.com")
                .password("hash")
                .role(RoleEnum.USER)
                .build();
        User savedOtherUser = userRepository.save(otherUser);
        String otherToken = jwtService.generateToken(savedOtherUser);

        Long lodgingId = createTestLodging();

        // Main user books +10..+12
        createReservation(lodgingId, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));

        // Other user books +20..+22 (non-overlapping)
        Map<String, Object> otherReq = Map.of(
                "lodgingId", lodgingId,
                "checkIn", LocalDate.now().plusDays(20).toString(),
                "checkOut", LocalDate.now().plusDays(22).toString(),
                "guestName", "Other User",
                "guestEmail", "other-isolation@test.com",
                "guestPhone", "+5491100000000"
        );
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/reservations")
                        .cookie(accessCookie(otherToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherReq)))
                .andExpect(status().isCreated());

        // Main user must only see their own reservation, not the other user's
        mockMvc.perform(get("/api/reservations/my")
                        .cookie(accessCookie(userAuthHeader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].guestEmail").value("juan-reservas@test.com"));
    }

    @Test
    void shouldReturnBadRequestWhenLodgingIsNotAvailable() throws Exception {
        Long lodgingId = createTestLodging();

        createReservation(lodgingId, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));

        Map<String, Object> request = Map.of(
                "lodgingId", lodgingId,
                "checkIn", LocalDate.now().plusDays(11).toString(),
                "checkOut", LocalDate.now().plusDays(13).toString(),
                "guestName", "Juan Perez",
                "guestEmail", "juan-reservas@test.com",
                "guestPhone", "+5491122334455"
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/reservations")
                        .cookie(accessCookie(userAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Real reservation-not-found path (ReservationServiceImpl.getReservationById), the
     * only production throw site for ResourceNotFoundException today. It uses the plain
     * `(String message)` constructor (no errorCode), so the handler's fallback branch —
     * `messageSource.getMessage("error.resource.not_found", new Object[]{ex.getMessage()},
     * locale)` — is the one actually exercised. `error.resource.not_found={0}` in both
     * bundles is a passthrough: the underlying message stays in whatever language it was
     * thrown in, only the wrapping resolution changes (proving the fallback resolves
     * instead of throwing an uncaught NoSuchMessageException, which would 500).
     */
    @Test
    void shouldReturnNotFoundWithOriginalMessageWhenAcceptLanguageIsEs() throws Exception {
        mockMvc.perform(get("/api/reservations/999999")
                        .cookie(accessCookie(userAuthHeader))
                        .header("Accept-Language", "es"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Reserva no encontrada con ID: 999999"));
    }

    @Test
    void shouldReturnNotFoundWithOriginalMessageWhenAcceptLanguageIsMissing() throws Exception {
        mockMvc.perform(get("/api/reservations/999999")
                        .cookie(accessCookie(userAuthHeader)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Reserva no encontrada con ID: 999999"));
    }
}
