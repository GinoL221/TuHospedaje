package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.common.PageResponse;
import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.lodging.LodgingSearchResponse;
import com.tuhospedaje.dto.lodging.RecommendationPageResponse;
import com.tuhospedaje.dto.reservation.AvailabilityResponse;
import com.tuhospedaje.dto.reservation.OccupiedRange;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.CityProjection;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.service.LodgingQueryService;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LodgingQueryServiceImpl implements LodgingQueryService {

    private static final int RANDOM_POOL_SIZE = 100;
    private static final int RANDOM_RESULT_SIZE = 10;
    private static final int MAX_UNFILTERED_RESULTS = 100;
    private static final Set<String> ADMIN_SORT_FIELDS = Set.of(
            "id", "name", "description", "city", "country", "pricePerNight"
    );

    private final LodgingRepository lodgingRepository;
    private final ReservationRepository reservationRepository;
    private final RatingRepository ratingRepository;

    public LodgingQueryServiceImpl(LodgingRepository lodgingRepository,
                                   ReservationRepository reservationRepository,
                                   RatingRepository ratingRepository) {
        this.lodgingRepository = lodgingRepository;
        this.reservationRepository = reservationRepository;
        this.ratingRepository = ratingRepository;
    }

    private List<LodgingDTO> enrichWithRatings(List<LodgingDTO> dtos) {
        if (dtos.isEmpty()) return dtos;
        Set<Long> ids = dtos.stream().map(LodgingDTO::getId).collect(Collectors.toSet());
        Map<Long, RatingRepository.RatingAggregate> byId = ratingRepository.aggregateByLodgingIds(ids).stream()
                .collect(Collectors.toMap(RatingRepository.RatingAggregate::getLodgingId, aggregate -> aggregate));
        for (LodgingDTO dto : dtos) {
            RatingRepository.RatingAggregate aggregate = byId.get(dto.getId());
            double average = aggregate != null && aggregate.getAverage() != null ? aggregate.getAverage() : 0.0;
            long count = aggregate != null ? aggregate.getCount() : 0L;
            dto.setRatingCount((int) count);
            dto.setAverageRating(Math.round(average * 10.0) / 10.0);
        }
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> findAll() {
        List<LodgingDTO> dtos = lodgingRepository.findAll(PageRequest.of(0, MAX_UNFILTERED_RESULTS)).getContent().stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        return enrichWithRatings(dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> findByCategory(Long categoryId) {
        List<LodgingDTO> dtos = lodgingRepository.findByCategoryId(categoryId).stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        return enrichWithRatings(dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Lodging> lodgingPage = lodgingRepository.findAll(pageable);
        List<LodgingDTO> lodgings = enrichWithRatings(lodgingPage.getContent().stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList()));
        Map<String, Object> response = new HashMap<>();
        response.put("lodgings", lodgings);
        response.put("currentPage", lodgingPage.getNumber());
        response.put("totalItems", lodgingPage.getTotalElements());
        response.put("totalPages", lodgingPage.getTotalPages());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LodgingDTO> findAdminPage(int page, int size, String sort, String direction, String query) {
        if (!ADMIN_SORT_FIELDS.contains(sort)) {
            throw new IllegalArgumentException("Campo de ordenamiento inválido: " + sort);
        }
        Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new IllegalArgumentException("Dirección de ordenamiento inválida: " + direction));
        Page<Lodging> lodgingPage = lodgingRepository.findAll(adminSearchSpec(query), PageRequest.of(page, size, Sort.by(sortDirection, sort)));
        List<LodgingDTO> lodgings = enrichWithRatings(lodgingPage.getContent().stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList()));
        return new PageResponse<>(lodgings, lodgingPage.getNumber(), lodgingPage.getTotalElements(), lodgingPage.getTotalPages());
    }

    private Specification<Lodging> adminSearchSpec(String query) {
        if (query == null || query.isBlank()) {
            return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("city")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("country")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), pattern)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationPageResponse findRecommendations(String seed, int page, int size, String requestedRevision) {
        List<Lodging> eligible = lodgingRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        String revision = recommendationRevision(eligible);
        boolean reset = requestedRevision != null && !requestedRevision.equals(revision);
        List<Lodging> ordered = new ArrayList<>(eligible);
        Collections.shuffle(ordered, new Random(recommendationSeed(seed, revision)));
        if (isDefaultIdOrder(ordered)) {
            Collections.rotate(ordered, 1);
        }
        int pageSize = Math.max(1, Math.min(size, RANDOM_RESULT_SIZE));
        int totalItems = ordered.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = totalPages == 0 ? 0 : reset ? 0 : Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = totalPages == 0 ? 0 : currentPage * pageSize;
        int toIndex = totalPages == 0 ? 0 : Math.min(fromIndex + pageSize, totalItems);
        List<LodgingDTO> lodgings = ordered.subList(fromIndex, toIndex).stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        return new RecommendationPageResponse(enrichWithRatings(lodgings), currentPage, totalItems, totalPages, revision, reset);
    }

    private String recommendationRevision(List<Lodging> eligible) {
        String ids = eligible.stream().map(Lodging::getId).map(String::valueOf).collect(Collectors.joining(","));
        return "v1-" + java.util.HexFormat.of().formatHex(sha256(ids));
    }

    private long recommendationSeed(String seed, String revision) {
        byte[] hash = sha256(seed + ":" + revision);
        long value = 0;
        for (int index = 0; index < Long.BYTES; index++) {
            value = (value << Byte.SIZE) | (hash[index] & 0xffL);
        }
        return value;
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private boolean isDefaultIdOrder(List<Lodging> lodgings) {
        for (int index = 1; index < lodgings.size(); index++) {
            if (lodgings.get(index - 1).getId().compareTo(lodgings.get(index).getId()) > 0) return false;
        }
        return lodgings.size() > 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> findAllRandom() {
        long total = lodgingRepository.count();
        if (total == 0) return List.of();
        int fetchSize = (int) Math.min(total, RANDOM_POOL_SIZE);
        List<Lodging> pool = new ArrayList<>(lodgingRepository.findAll(PageRequest.of(0, fetchSize)).getContent());
        Collections.shuffle(pool);
        List<LodgingDTO> dtos = pool.stream().limit(RANDOM_RESULT_SIZE)
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        return enrichWithRatings(dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public LodgingSearchResponse search(String city, LocalDate checkIn, LocalDate checkOut, Integer guests,
                                        List<Long> categories, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        Specification<Lodging> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
        }
        if (guests != null) spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("maxGuests"), guests));
        if (categories != null && !categories.isEmpty()) spec = spec.and((root, query, criteriaBuilder) -> root.get("category").get("id").in(categories));
        if (minPrice != null) spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice));
        if (maxPrice != null) spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice));
        if (checkIn != null && checkOut != null) {
            spec = spec.and((root, query, criteriaBuilder) -> {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Reservation> reservation = subquery.from(Reservation.class);
                subquery.select(reservation.get("id")).where(
                        criteriaBuilder.equal(reservation.get("lodging"), root),
                        criteriaBuilder.equal(reservation.get("status"), ReservationStatus.CONFIRMED),
                        criteriaBuilder.lessThan(reservation.get("checkIn"), checkOut),
                        criteriaBuilder.greaterThan(reservation.get("checkOut"), checkIn));
                return criteriaBuilder.not(criteriaBuilder.exists(subquery));
            });
        }
        Page<Lodging> resultsPage = lodgingRepository.findAll(spec, PageRequest.of(page, size));
        List<LodgingDTO> lodgings = enrichWithRatings(resultsPage.getContent().stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList()));
        return new LodgingSearchResponse(lodgings, resultsPage.getNumber(), resultsPage.getTotalElements(), resultsPage.getTotalPages(), lodgingRepository.count());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findCities(String query) {
        return lodgingRepository.findDistinctByCityContainingIgnoreCaseOrderByCityAsc(query == null ? "" : query).stream()
                .map(CityProjection::getCity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(Long lodgingId, LocalDate checkIn, LocalDate checkOut) {
        lodgingRepository.findById(lodgingId).orElseThrow(() -> new ResourceNotFoundException(
                "error.lodging.not_found", new Object[]{lodgingId}, "Lodging not found with ID: " + lodgingId));
        List<Reservation> confirmed = reservationRepository.findByLodgingIdAndStatus(lodgingId, ReservationStatus.CONFIRMED);
        List<OccupiedRange> occupiedRanges = confirmed.stream().map(reservation -> {
            OccupiedRange range = new OccupiedRange();
            range.setCheckIn(reservation.getCheckIn());
            range.setCheckOut(reservation.getCheckOut());
            return range;
        }).toList();
        boolean available = checkIn == null || checkOut == null || confirmed.stream()
                .noneMatch(reservation -> reservation.getCheckIn().isBefore(checkOut) && reservation.getCheckOut().isAfter(checkIn));
        AvailabilityResponse response = new AvailabilityResponse();
        response.setAvailable(available);
        response.setOccupiedRanges(occupiedRanges);
        return response;
    }
}
