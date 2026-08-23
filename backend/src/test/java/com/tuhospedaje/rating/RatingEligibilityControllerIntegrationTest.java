package com.tuhospedaje.rating;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.rating.RatingRequest;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real-database coverage: authenticated eligibility read, checkout-boundary-day and
 * cancelled-reservation exclusion, anonymous denial, and rejection of an ineligible
 * POST and real-database user/lodging query scoping.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RatingEligibilityControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LodgingRepository lodgingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private JwtService jwtService;
    @Autowired private Clock clock;

    private User user;
    private String authHeader;
    private Lodging lodging;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        lodgingRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .firstName("Elena")
                .lastName("Rios")
                .email("elena-eligibility-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build());
        authHeader = jwtService.generateToken(user);

        lodging = new Lodging();
        lodging.setName("Hostel Andes");
        lodging.setAddress("Av. Siempreviva 742");
        lodging.setCity("Springfield");
        lodging.setCountry("USA");
        lodging.setPhoneNumber("555-0199");
        lodging.setEmail("hostel-andes-eligibility@test.com");
        lodging.setPricePerNight(new BigDecimal("80.00"));
        lodging.setMaxGuests(2);
        lodging = lodgingRepository.save(lodging);
    }

    @Test
    void eligibleUser_readsEligibilityAsTrue() throws Exception {
        seedReservation(ReservationStatus.CONFIRMED, LocalDate.now(clock).minusDays(2));

        mockMvc.perform(get("/api/ratings/lodging/{lodgingId}/eligibility", lodging.getId())
                        .cookie(accessCookie(authHeader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.reason").value("ELIGIBLE"));
    }

    @Test
    void checkoutBoundaryAndCancelledReservation_returnIneligible() throws Exception {
        seedReservation(ReservationStatus.CONFIRMED, LocalDate.now(clock));
        assertEligibility(false);

        reservationRepository.deleteAll();
        seedReservation(ReservationStatus.CANCELLED, LocalDate.now(clock).minusDays(2));
        assertEligibility(false);
    }

    @Test
    void qualifyingStayForDifferentUserOrLodging_doesNotGrantEligibility() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .firstName("Mateo")
                .lastName("Luna")
                .email("mateo-eligibility-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build());
        seedReservation(otherUser, lodging, ReservationStatus.CONFIRMED, LocalDate.now(clock).minusDays(2));
        assertEligibility(false);

        Lodging otherLodging = new Lodging();
        otherLodging.setName("Posada Norte");
        otherLodging.setAddress("Calle Norte 12");
        otherLodging.setCity("Springfield");
        otherLodging.setCountry("USA");
        otherLodging.setPhoneNumber("555-0188");
        otherLodging.setEmail("posada-norte-eligibility@test.com");
        otherLodging.setPricePerNight(new BigDecimal("90.00"));
        otherLodging.setMaxGuests(2);
        otherLodging = lodgingRepository.save(otherLodging);
        seedReservation(user, otherLodging, ReservationStatus.CONFIRMED, LocalDate.now(clock).minusDays(2));
        assertEligibility(false);
    }

    @Test
    void anonymousRequest_isDenied() throws Exception {
        mockMvc.perform(get("/api/ratings/lodging/{lodgingId}/eligibility", lodging.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void ineligiblePost_isRejectedWithValidationEnvelope() throws Exception {
        seedReservation(ReservationStatus.CONFIRMED, LocalDate.now(clock).plusDays(1));

        RatingRequest request = new RatingRequest();
        request.setLodgingId(lodging.getId());
        request.setScore(5);
        request.setComment("Todavía no me hospedé");

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/ratings")
                        .cookie(accessCookie(authHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists());
    }

    private void assertEligibility(boolean expected) throws Exception {
        mockMvc.perform(get("/api/ratings/lodging/{lodgingId}/eligibility", lodging.getId())
                        .cookie(accessCookie(authHeader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(expected));
    }

    private void seedReservation(ReservationStatus status, LocalDate checkOut) {
        seedReservation(user, lodging, status, checkOut);
    }

    private void seedReservation(User reservationUser, Lodging reservationLodging,
                                 ReservationStatus status, LocalDate checkOut) {
        Reservation reservation = new Reservation();
        reservation.setLodging(reservationLodging);
        reservation.setUser(reservationUser);
        reservation.setCheckIn(checkOut.minusDays(3));
        reservation.setCheckOut(checkOut);
        reservation.setGuestName(reservationUser.getFirstName() + " " + reservationUser.getLastName());
        reservation.setGuestEmail(reservationUser.getEmail());
        reservation.setGuestPhone("555-0100");
        reservation.setTotalPrice(new BigDecimal("240.00"));
        reservation.setStatus(status);
        reservationRepository.save(reservation);
    }
}
