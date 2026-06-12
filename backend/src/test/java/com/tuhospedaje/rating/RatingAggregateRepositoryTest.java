package com.tuhospedaje.rating;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Rating;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SC-3.1, SC-3.2, SC-3.3: RatingRepository.aggregateByLodgingIds JPQL query.
 * Verifies: single aggregate per call, correct rounding, zero-safe defaults.
 */
@SpringBootTest
class RatingAggregateRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    private Lodging lodgingA;
    private Lodging lodgingB;
    private User rater;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        lodgingRepository.deleteAll();
        userRepository.deleteAll();

        rater = userRepository.save(User.builder()
                .firstName("Rater")
                .lastName("User")
                .email("rater-agg@test.com")
                .password("hash")
                .role(RoleEnum.USER)
                .build());

        lodgingA = lodgingRepository.save(buildLodging("Lodging A", "agg-a@test.com"));
        lodgingB = lodgingRepository.save(buildLodging("Lodging B", "agg-b@test.com"));
    }

    /** SC-3.2: avg 4.0 case */
    @Test
    void aggregateByLodgingIds_returnsCorrectAvg_4_0() {
        addRating(lodgingA, 4);
        addRating(lodgingA, 3);
        addRating(lodgingA, 5);
        addRating(lodgingA, 4);

        var results = ratingRepository.aggregateByLodgingIds(Set.of(lodgingA.getId()));
        assertThat(results).hasSize(1);
        RatingRepository.RatingAggregate agg = results.get(0);
        assertThat(agg.getLodgingId()).isEqualTo(lodgingA.getId());
        double rounded = Math.round(agg.getAverage() * 10.0) / 10.0;
        assertThat(rounded).isEqualTo(4.0);
        assertThat(agg.getCount()).isEqualTo(4L);
    }

    /** SC-3.2: avg 3.5 case */
    @Test
    void aggregateByLodgingIds_returnsCorrectAvg_3_5() {
        addRating(lodgingA, 3);
        addRating(lodgingA, 4);

        var results = ratingRepository.aggregateByLodgingIds(Set.of(lodgingA.getId()));
        assertThat(results).hasSize(1);
        double rounded = Math.round(results.get(0).getAverage() * 10.0) / 10.0;
        assertThat(rounded).isEqualTo(3.5);
    }

    /** SC-3.2: avg 3.666... rounds to 3.7 */
    @Test
    void aggregateByLodgingIds_returnsCorrectAvg_3_7_rounding() {
        addRating(lodgingA, 3);
        addRating(lodgingA, 4);
        addRating(lodgingA, 4);

        var results = ratingRepository.aggregateByLodgingIds(Set.of(lodgingA.getId()));
        double rounded = Math.round(results.get(0).getAverage() * 10.0) / 10.0;
        assertThat(rounded).isEqualTo(3.7);
    }

    /** SC-3.3: lodging with zero ratings is ABSENT from result → post-fill to 0.0/0 at service layer */
    @Test
    void aggregateByLodgingIds_absentForUnratedLodging() {
        // lodgingB has no ratings
        var results = ratingRepository.aggregateByLodgingIds(Set.of(lodgingB.getId()));
        assertThat(results).isEmpty();
    }

    /** SC-3.1 proxy: batch query returns aggregates for multiple lodgings in one call */
    @Test
    void aggregateByLodgingIds_returnsMultipleLodgingsInOneBatch() {
        addRating(lodgingA, 5);
        addRating(lodgingB, 3);
        addRating(lodgingB, 4);

        var results = ratingRepository.aggregateByLodgingIds(Set.of(lodgingA.getId(), lodgingB.getId()));
        assertThat(results).hasSize(2);
    }

    /** Empty ids set guard: skip query and return empty */
    @Test
    void aggregateByLodgingIds_emptyIdsSet_returnsEmpty() {
        var results = ratingRepository.aggregateByLodgingIds(Set.of());
        assertThat(results).isEmpty();
    }

    private Lodging buildLodging(String name, String email) {
        Lodging l = new Lodging();
        l.setName(name);
        l.setDescription("desc");
        l.setAddress("addr");
        l.setCity("city");
        l.setCountry("country");
        l.setPhoneNumber("123");
        l.setEmail(email);
        l.setPricePerNight(new BigDecimal("80.00"));
        l.setMaxGuests(2);
        return l;
    }

    private void addRating(Lodging lodging, int score) {
        Rating r = new Rating();
        r.setLodging(lodging);
        r.setUser(rater);
        r.setScore(score);
        ratingRepository.save(r);
    }
}
