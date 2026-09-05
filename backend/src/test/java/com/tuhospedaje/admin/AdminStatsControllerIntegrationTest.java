package com.tuhospedaje.admin;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.LodgingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The admin dashboard used to derive its five stat cards by downloading each table whole
 * and reading {@code .length} client-side. That is why this endpoint exists: counts are a
 * {@code SELECT COUNT(*)}, not a payload, and the lodging card was showing a capped —
 * i.e. wrong — number.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminStatsControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private LodgingService lodgingService;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        lodgingRepository.deleteAll();
        featureRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .firstName("Admin").lastName("Stats")
                .email("admin-stats@test.com").password("hash")
                .role(RoleEnum.ADMIN).build());
        User user = userRepository.save(User.builder()
                .firstName("Plain").lastName("Stats")
                .email("user-stats@test.com").password("hash")
                .role(RoleEnum.USER).build());

        adminToken = jwtService.generateToken(admin);
        userToken = jwtService.generateToken(user);
    }

    @Test
    void stats_reportOneCountPerAggregate() throws Exception {
        seedCategories(3);
        seedFeatures(4);
        seedLodgings(2);

        mockMvc.perform(get("/api/admin/stats").cookie(accessCookie(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings").value(2))
                .andExpect(jsonPath("$.categories").value(3))
                .andExpect(jsonPath("$.features").value(4))
                .andExpect(jsonPath("$.users").value(2))
                .andExpect(jsonPath("$.reservations").value(0));
    }

    /**
     * The defect this endpoint replaces: the dashboard counted the rows returned by
     * {@code GET /api/lodgings}, which {@code LodgingServiceImpl.findAll} caps at
     * MAX_UNFILTERED_RESULTS. Past that cap the card silently displayed the cap instead of
     * the truth. The count must not inherit that ceiling.
     */
    @Test
    void lodgingCount_isNotCappedByTheUnfilteredListingLimit() throws Exception {
        seedCategories(1);
        int beyondTheListingCap = 101;
        seedLodgings(beyondTheListingCap);

        assertThat(lodgingService.findAll())
                .as("precondition: the listing this replaced is capped, so it cannot be counted")
                .hasSizeLessThan(beyondTheListingCap);

        mockMvc.perform(get("/api/admin/stats").cookie(accessCookie(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings").value(beyondTheListingCap));
    }

    @Test
    void stats_areForbiddenForAuthenticatedNonAdmins() throws Exception {
        mockMvc.perform(get("/api/admin/stats").cookie(accessCookie(userToken)))
                .andExpect(status().isForbidden());
    }

    /**
     * 403, not 401: SecurityConfig deliberately answers unauthenticated requests with 403
     * everywhere except the four paths a spec requirement pins to 401
     * ({@code /api/reservations/**}, {@code /api/auth/me}, {@code /api/auth/csrf},
     * {@code /api/auth/welcome-email/resend}). This endpoint follows the house rule rather
     * than extending that exception list.
     */
    @Test
    void stats_areDeniedForAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isForbidden());
    }

    private void seedCategories(int count) {
        for (int i = 0; i < count; i++) {
            Category category = new Category();
            category.setName("stats-category-" + i);
            category.setDescription("Fixture category " + i);
            categoryRepository.save(category);
        }
    }

    private void seedFeatures(int count) {
        for (int i = 0; i < count; i++) {
            Feature feature = new Feature();
            feature.setName("stats-feature-" + i);
            feature.setIcon("icon-" + i);
            featureRepository.save(feature);
        }
    }

    private void seedLodgings(int count) {
        for (int i = 0; i < count; i++) {
            Lodging lodging = new Lodging();
            lodging.setName("Stats Lodging " + i);
            lodging.setAddress("Street " + i);
            lodging.setCity("stats-city");
            lodging.setCountry("Argentina");
            lodging.setPhoneNumber("+54 11 0000 " + i);
            lodging.setEmail("stats-lodging-" + i + "@stats.test");
            lodging.setPricePerNight(new BigDecimal("100.00"));
            lodging.setMaxGuests(4);
            lodgingRepository.save(lodging);
        }
    }
}
