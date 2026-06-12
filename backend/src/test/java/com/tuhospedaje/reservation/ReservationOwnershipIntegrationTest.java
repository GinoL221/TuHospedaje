package com.tuhospedaje.reservation;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SC-2.1 through SC-2.5 — IDOR ownership enforcement for GET /api/reservations/{id}.
 * REQ-2: non-owner non-admin gets 404 (hides resource existence).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationOwnershipIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    private User owner;
    private User otherUser;
    private User admin;
    private Long reservationId;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        lodgingRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .firstName("Owner")
                .lastName("User")
                .email("owner-idor@test.com")
                .password("hash")
                .role(RoleEnum.USER)
                .build());

        otherUser = userRepository.save(User.builder()
                .firstName("Other")
                .lastName("User")
                .email("other-idor@test.com")
                .password("hash")
                .role(RoleEnum.USER)
                .build());

        admin = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin-idor@test.com")
                .password("hash")
                .role(RoleEnum.ADMIN)
                .build());

        Lodging lodging = new Lodging();
        lodging.setName("IDOR Test Hotel");
        lodging.setDescription("desc");
        lodging.setAddress("addr");
        lodging.setCity("city");
        lodging.setCountry("country");
        lodging.setPhoneNumber("123");
        lodging.setEmail("idor-hotel@test.com");
        lodging.setPricePerNight(new BigDecimal("100.00"));
        lodging.setMaxGuests(2);
        Lodging savedLodging = lodgingRepository.save(lodging);

        Reservation reservation = new Reservation();
        reservation.setLodging(savedLodging);
        reservation.setUser(owner);
        reservation.setCheckIn(LocalDate.now().plusDays(5));
        reservation.setCheckOut(LocalDate.now().plusDays(7));
        reservation.setGuestName("Owner User");
        reservation.setGuestEmail("owner-idor@test.com");
        reservation.setGuestPhone("555-0001");
        reservation.setTotalPrice(new BigDecimal("200.00"));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationId = reservationRepository.save(reservation).getId();
    }

    /** SC-2.1: owner retrieves own reservation → 200 */
    @Test
    void ownerGetsOwnReservation_returns200() throws Exception {
        String token = "Bearer " + jwtService.generateToken(owner);

        mockMvc.perform(get("/api/reservations/{id}", reservationId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId));
    }

    /** SC-2.2: admin retrieves any reservation → 200 */
    @Test
    void adminGetsAnyReservation_returns200() throws Exception {
        String token = "Bearer " + jwtService.generateToken(admin);

        mockMvc.perform(get("/api/reservations/{id}", reservationId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId));
    }

    /** SC-2.3: non-owner non-admin (user A requests user B's reservation) → 404 */
    @Test
    void nonOwnerNonAdmin_gets404_notRevealingExistence() throws Exception {
        String token = "Bearer " + jwtService.generateToken(otherUser);

        mockMvc.perform(get("/api/reservations/{id}", reservationId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    /** SC-2.4: unauthenticated request → 401 (entry point fires before ownership check) */
    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/reservations/{id}", reservationId))
                .andExpect(status().isUnauthorized());
    }

    /** SC-2.5: non-existent reservation → 404 regardless of caller */
    @Test
    void nonExistentReservation_returns404() throws Exception {
        String token = "Bearer " + jwtService.generateToken(owner);

        mockMvc.perform(get("/api/reservations/{id}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }
}
