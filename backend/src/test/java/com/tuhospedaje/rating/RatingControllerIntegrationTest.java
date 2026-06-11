package com.tuhospedaje.rating;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.rating.RatingRequest;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Rating;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RatingControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private String userAuthHeader;
    private Lodging testLodging;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        lodgingRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .firstName("Gino")
                .lastName("PC")
                .email("gino-rating-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build();
        testUser = userRepository.save(testUser);
        userAuthHeader = "Bearer " + jwtService.generateToken(testUser);

        testLodging = new Lodging();
        testLodging.setName("Hostel Oasis");
        testLodging.setDescription("Beautiful oasis");
        testLodging.setAddress("Av. Siempreviva 742");
        testLodging.setCity("Springfield");
        testLodging.setCountry("USA");
        testLodging.setPhoneNumber("555-0199");
        testLodging.setEmail("oasis@oasis.com");
        testLodging.setPricePerNight(new BigDecimal("80.00"));
        testLodging.setMaxGuests(2);
        testLodging = lodgingRepository.save(testLodging);

        // Seed a confirmed reservation so the user is eligible to rate
        Reservation reservation = new Reservation();
        reservation.setLodging(testLodging);
        reservation.setUser(testUser);
        reservation.setCheckIn(LocalDate.now().minusDays(5));
        reservation.setCheckOut(LocalDate.now().minusDays(2));
        reservation.setGuestName("Gino PC");
        reservation.setGuestEmail("gino-rating-test@tuhospedaje.com");
        reservation.setGuestPhone("555-0199");
        reservation.setTotalPrice(new BigDecimal("240.00"));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
    }

    @Test
    void shouldCreateNewRatingSuccessfully() throws Exception {
        RatingRequest request = new RatingRequest();
        request.setLodgingId(testLodging.getId());
        request.setScore(5);
        request.setComment("Excelente lugar!");

        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<Rating> ratings = ratingRepository.findAll();
        assertThat(ratings).hasSize(1);
        assertThat(ratings.get(0).getScore()).isEqualTo(5);
        assertThat(ratings.get(0).getComment()).isEqualTo("Excelente lugar!");
    }

    @Test
    void shouldUpsertRatingWhenRatingAlreadyExists() throws Exception {
        // Submit first rating
        RatingRequest firstRequest = new RatingRequest();
        firstRequest.setLodgingId(testLodging.getId());
        firstRequest.setScore(4);
        firstRequest.setComment("Muy bueno");

        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Submitting rating again should update the existing rating
        RatingRequest secondRequest = new RatingRequest();
        secondRequest.setLodgingId(testLodging.getId());
        secondRequest.setScore(2);
        secondRequest.setComment("Cambió el servicio, ahora es malo");

        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        // Assert only one rating exists, with updated values
        List<Rating> ratings = ratingRepository.findAll();
        assertThat(ratings).hasSize(1);
        assertThat(ratings.get(0).getScore()).isEqualTo(2);
        assertThat(ratings.get(0).getComment()).isEqualTo("Cambió el servicio, ahora es malo");
    }
}
