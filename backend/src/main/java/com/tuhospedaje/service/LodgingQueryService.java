package com.tuhospedaje.service;

import com.tuhospedaje.dto.common.PageResponse;
import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.lodging.LodgingSearchResponse;
import com.tuhospedaje.dto.lodging.RecommendationPageResponse;
import com.tuhospedaje.dto.reservation.AvailabilityResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LodgingQueryService {
    List<LodgingDTO> findAll();

    List<LodgingDTO> findByCategory(Long categoryId);

    Map<String, Object> findAllPaginated(int page, int size);

    PageResponse<LodgingDTO> findAdminPage(int page, int size, String sort, String direction, String query);

    List<LodgingDTO> findAllRandom();

    RecommendationPageResponse findRecommendations(String seed, int page, int size, String revision);

    LodgingSearchResponse search(String city, LocalDate checkIn, LocalDate checkOut,
                                 Integer guests, List<Long> categories,
                                 BigDecimal minPrice, BigDecimal maxPrice,
                                 int page, int size);

    List<String> findCities(String query);

    AvailabilityResponse checkAvailability(Long lodgingId, LocalDate checkIn, LocalDate checkOut);
}
