package com.tuhospedaje.lodging;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.lodging.RecommendationPageResponse;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.service.impl.LodgingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceContractTest {

    @Mock
    private LodgingRepository lodgingRepository;

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private LodgingServiceImpl lodgingService;

    @Test
    void keepsFixedSeedPagesStableUniqueAndCappedAtTen() {
        when(lodgingRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(lodgings(12));
        when(ratingRepository.aggregateByLodgingIds(any())).thenReturn(List.of());

        RecommendationPageResponse firstPage = lodgingService.findRecommendations("recommendationseed", 0, 10, null);
        RecommendationPageResponse secondPage = lodgingService.findRecommendations("recommendationseed", 1, 10, firstPage.revision());
        RecommendationPageResponse revisitedFirstPage = lodgingService.findRecommendations(
                "recommendationseed", 0, 10, firstPage.revision());

        assertThat(firstPage.lodgings()).hasSize(10);
        assertThat(firstPage.lodgings()).extracting(dto -> dto.getId()).doesNotHaveDuplicates();
        assertThat(firstPage.lodgings()).extracting(dto -> dto.getId())
                .containsExactlyElementsOf(revisitedFirstPage.lodgings().stream().map(dto -> dto.getId()).toList());
        assertThat(firstPage.lodgings()).extracting(dto -> dto.getId())
                .doesNotContainAnyElementsOf(secondPage.lodgings().stream().map(dto -> dto.getId()).toList());
        assertThat(firstPage.lodgings()).extracting(dto -> dto.getId())
                .isNotEqualTo(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
    }

    @Test
    void clampsOutOfRangeAndAtomicallyResetsOnRevisionMismatch() {
        when(lodgingRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(lodgings(2));
        when(ratingRepository.aggregateByLodgingIds(any())).thenReturn(List.of());

        RecommendationPageResponse initial = lodgingService.findRecommendations("recommendationseed", 99, 10, null);
        RecommendationPageResponse reset = lodgingService.findRecommendations("recommendationseed", 1, 10, "obsolete-revision");

        assertThat(initial.currentPage()).isZero();
        assertThat(initial.totalPages()).isEqualTo(1);
        assertThat(reset.currentPage()).isZero();
        assertThat(reset.reset()).isTrue();
        assertThat(reset.lodgings()).extracting(dto -> dto.getId()).doesNotHaveDuplicates();
    }

    @Test
    void clampsNegativeDirectServicePageToFirstPage() {
        when(lodgingRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(lodgings(12));
        when(ratingRepository.aggregateByLodgingIds(any())).thenReturn(List.of());

        RecommendationPageResponse page = lodgingService.findRecommendations("recommendationseed", -1, 10, null);

        assertThat(page.currentPage()).isZero();
        assertThat(page.lodgings()).hasSize(10);
    }

    @Test
    void returnsBoundedEmptyPageAndCapsServiceSize() {
        when(lodgingRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of());
        when(ratingRepository.aggregateByLodgingIds(any())).thenReturn(List.of());

        RecommendationPageResponse empty = lodgingService.findRecommendations("recommendationseed", 4, 100, null);

        assertThat(empty.lodgings()).isEmpty();
        assertThat(empty.currentPage()).isZero();
        assertThat(empty.totalItems()).isZero();
        assertThat(empty.totalPages()).isZero();

        when(lodgingRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(lodgings(12));
        RecommendationPageResponse capped = lodgingService.findRecommendations("recommendationseed", 0, 100, null);

        assertThat(capped.lodgings()).hasSize(10);
        assertThat(capped.totalPages()).isEqualTo(2);
    }

    private List<Lodging> lodgings(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> {
                    Lodging lodging = new Lodging();
                    lodging.setId((long) index);
                    lodging.setName("Lodging " + index);
                    lodging.setAddress("Address " + index);
                    lodging.setCity("City");
                    lodging.setCountry("Country");
                    lodging.setPhoneNumber("555" + index);
                    lodging.setEmail("lodging" + index + "@example.test");
                    lodging.setPricePerNight(BigDecimal.TEN);
                    lodging.setMaxGuests(2);
                    return lodging;
                })
                .toList();
    }
}

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationControllerContractTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesValidatedRecommendationResponseContract() throws Exception {
        mockMvc.perform(get("/api/lodgings/recommendations")
                        .param("seed", "recommendationseed")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgings").isArray())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.revision").isString())
                .andExpect(jsonPath("$.reset").value(false));
    }

    @Test
    void rejectsInvalidRecommendationParameters() throws Exception {
        mockMvc.perform(get("/api/lodgings/recommendations").param("seed", "too-short"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/lodgings/recommendations")
                        .param("seed", "recommendationseed")
                        .param("size", "11"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/lodgings/recommendations")
                        .param("seed", "recommendationseed")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
