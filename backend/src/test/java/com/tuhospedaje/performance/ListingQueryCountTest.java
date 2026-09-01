package com.tuhospedaje.performance;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.LodgingImage;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.PolicyRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.LodgingService;
import com.tuhospedaje.service.ReservationService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioral guard against N+1 on the listing paths.
 *
 * <p>The property under test is not "few queries" — it is that the query count does NOT
 * grow with the number of rows returned. A threshold test passes for the wrong reason as
 * soon as someone changes the fixture size; comparing a small page against a larger one
 * fails precisely when a lazy association starts being loaded per row.
 *
 * <p>This measures {@link Statistics#getPrepareStatementCount()}, NOT
 * {@code getQueryExecutionCount()}. That distinction is the whole point:
 * {@code getQueryExecutionCount} counts HQL/criteria executions and does not see lazy
 * association loads at all, which is why {@code
 * LodgingControllerIntegrationTest.searchWithDates_executesAtMostTwoQueries} kept passing
 * while every listing paid four extra selects per lodging.
 *
 * <p>IMPORTANT: does NOT extend {@code AbstractIntegrationTest}. Its class-level
 * {@code @Transactional} would share one persistence context between the seeding and the
 * service call, so the seeded entities would be served from the first-level cache and the
 * lazy loads being measured would never happen. Cleanup is therefore manual.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ListingQueryCountTest {

    private static final int SMALL_PAGE = 2;
    private static final int LARGER_PAGE = 8;

    @Autowired
    private LodgingService lodgingService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        cleanAll();
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    /**
     * {@code LodgingDTO.fromEntity} reads category, features, policies and images — four
     * lazy associations. Without batch fetching each one costs a select per lodging, so a
     * page of 8 pays 32 extra round-trips that a page of 2 does not.
     */
    @Test
    void findAll_queryCountIsIndependentOfHowManyLodgingsAreReturned() {
        seedLodgings(SMALL_PAGE);
        long smallPageStatements = statementsFor(() -> lodgingService.findAll());

        cleanAll();
        seedLodgings(LARGER_PAGE);
        long largerPageStatements = statementsFor(() -> lodgingService.findAll());

        assertThat(largerPageStatements)
                .as("a listing of %d lodgings must not cost more queries than one of %d",
                        LARGER_PAGE, SMALL_PAGE)
                .isEqualTo(smallPageStatements);
        // Equality alone would still hold if both sides regressed together (e.g. a new
        // per-call query added outside the row loop), so pin the absolute cost too:
        // the page itself, one batch per lazy association, and the ratings aggregate.
        assertThat(largerPageStatements).isLessThanOrEqualTo(8);
    }

    /** Same associations, and the path the public catalog filter actually uses. */
    @Test
    void findByCategory_queryCountIsIndependentOfHowManyLodgingsAreReturned() {
        Long categoryId = seedLodgings(SMALL_PAGE).getId();
        long smallPageStatements = statementsFor(() -> lodgingService.findByCategory(categoryId));

        cleanAll();
        Long largerCategoryId = seedLodgings(LARGER_PAGE).getId();
        long largerPageStatements = statementsFor(() -> lodgingService.findByCategory(largerCategoryId));

        assertThat(largerPageStatements).isEqualTo(smallPageStatements);
    }

    /**
     * {@code ReservationResponse.fromEntity} calls {@code getLodging().getName()}, which
     * initializes the LAZY proxy — one select per reservation. Reading the id alone would
     * have been proxy-safe; reading any other column is not.
     */
    @Test
    void getAllReservations_queryCountIsIndependentOfHowManyReservationsAreReturned() {
        seedReservations(SMALL_PAGE);
        long smallPageStatements = statementsFor(() -> reservationService.getAllReservations());

        cleanAll();
        seedReservations(LARGER_PAGE);
        long largerPageStatements = statementsFor(() -> reservationService.getAllReservations());

        assertThat(largerPageStatements)
                .as("reservation rows each resolve their lodging proxy; that must be batched")
                .isEqualTo(smallPageStatements);
    }

    private long statementsFor(Runnable action) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }

    /** Returns the category every seeded lodging belongs to. */
    private Category seedLodgings(int count) {
        Category category = new Category();
        category.setName("Batch Fetch Category");
        category.setDescription("Fixture for query-count assertions");
        category = categoryRepository.save(category);

        Set<Feature> features = new LinkedHashSet<>();
        for (int i = 0; i < 3; i++) {
            Feature feature = new Feature();
            feature.setName("batch-feature-" + i);
            feature.setIcon("icon-" + i);
            features.add(featureRepository.save(feature));
        }

        Set<Policy> policies = new LinkedHashSet<>();
        for (int i = 0; i < 2; i++) {
            Policy policy = new Policy();
            policy.setName("batch-policy-" + i);
            policy.setDescription("Fixture policy " + i);
            policy.setIcon("icon-" + i);
            policies.add(policyRepository.save(policy));
        }

        for (int i = 0; i < count; i++) {
            Lodging lodging = new Lodging();
            lodging.setName("Batch Lodging " + i);
            lodging.setDescription("Fixture lodging " + i);
            lodging.setAddress("Street " + i);
            lodging.setCity("batch-city");
            lodging.setCountry("Argentina");
            lodging.setPhoneNumber("+54 11 0000 000" + i);
            lodging.setEmail("batch-lodging-" + i + "@query-count.test");
            lodging.setPricePerNight(new BigDecimal("100.00"));
            lodging.setMaxGuests(4);
            lodging.setCategory(category);
            lodging.setFeatures(new LinkedHashSet<>(features));
            lodging.setPolicies(new LinkedHashSet<>(policies));

            List<LodgingImage> images = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                LodgingImage image = new LodgingImage();
                image.setLodging(lodging);
                image.setImageUrl("https://images.test/lodging-" + i + "-" + j + ".jpg");
                images.add(image);
            }
            lodging.setImages(images);

            lodgingRepository.save(lodging);
        }
        return category;
    }

    private void seedReservations(int count) {
        seedLodgings(count);
        List<Lodging> lodgings = lodgingRepository.findAll();

        User guest = userRepository.save(User.builder()
                .firstName("Query")
                .lastName("Count")
                .email("query-count-guest@query-count.test")
                .password("hash")
                .role(RoleEnum.USER)
                .build());

        LocalDate today = LocalDate.now();
        for (int i = 0; i < lodgings.size(); i++) {
            Reservation reservation = new Reservation();
            reservation.setLodging(lodgings.get(i));
            reservation.setUser(guest);
            reservation.setCheckIn(today.plusDays(1L + i * 10L));
            reservation.setCheckOut(today.plusDays(4L + i * 10L));
            reservation.setGuestName("Guest " + i);
            reservation.setGuestEmail("guest-" + i + "@query-count.test");
            reservation.setGuestPhone("+54 11 1111 111" + i);
            reservation.setCreatedAt(LocalDateTime.now());
            reservation.setCreatedAtDerived(false);
            reservation.setTotalPrice(new BigDecimal("300.00"));
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
        }
    }

    private void cleanAll() {
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        lodgingRepository.deleteAll();
        featureRepository.deleteAll();
        policyRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }
}
