package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.common.PageResponse;
import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.lodging.LodgingSearchResponse;
import com.tuhospedaje.dto.lodging.RecommendationPageResponse;
import com.tuhospedaje.dto.reservation.AvailabilityResponse;
import com.tuhospedaje.dto.reservation.OccupiedRange;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.LodgingImage;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.CityProjection;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.PolicyRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.service.LodgingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LodgingServiceImpl implements LodgingService {

    private static final int RANDOM_POOL_SIZE = 100;
    private static final int RANDOM_RESULT_SIZE = 10;
    private static final int MAX_UNFILTERED_RESULTS = 100;
    private static final Set<String> ADMIN_SORT_FIELDS = Set.of(
            "id", "name", "description", "city", "country", "pricePerNight"
    );

    private final LodgingRepository lodgingRepository;
    private final CategoryRepository categoryRepository;
    private final FeatureRepository featureRepository;
    private final ReservationRepository reservationRepository;
    private final PolicyRepository policyRepository;
    private final RatingRepository ratingRepository;

    public LodgingServiceImpl(LodgingRepository lodgingRepository, CategoryRepository categoryRepository,
                              FeatureRepository featureRepository, ReservationRepository reservationRepository,
                              PolicyRepository policyRepository, RatingRepository ratingRepository) {
        this.lodgingRepository = lodgingRepository;
        this.categoryRepository = categoryRepository;
        this.featureRepository = featureRepository;
        this.reservationRepository = reservationRepository;
        this.policyRepository = policyRepository;
        this.ratingRepository = ratingRepository;
    }

    /**
     * Batch enricher: issues exactly 1 aggregate query for the full list.
     * Lodgings absent from the query result (no ratings) are post-filled with 0.0/0.
     */
    private List<LodgingDTO> enrichWithRatings(List<LodgingDTO> dtos) {
        if (dtos.isEmpty()) return dtos;
        Set<Long> ids = dtos.stream().map(LodgingDTO::getId).collect(Collectors.toSet());
        Map<Long, RatingRepository.RatingAggregate> byId =
                ratingRepository.aggregateByLodgingIds(ids).stream()
                        .collect(Collectors.toMap(RatingRepository.RatingAggregate::getLodgingId, a -> a));
        for (LodgingDTO dto : dtos) {
            RatingRepository.RatingAggregate a = byId.get(dto.getId());
            double avg = (a != null && a.getAverage() != null) ? a.getAverage() : 0.0;
            long count = (a != null) ? a.getCount() : 0L;
            dto.setRatingCount((int) count);
            dto.setAverageRating(Math.round(avg * 10.0) / 10.0);
        }
        return dtos;
    }

    /** Single-item adapter — reuses batch enricher (one aggregate query for one id). */
    private LodgingDTO enrichWithRatings(LodgingDTO dto) {
        return enrichWithRatings(new ArrayList<>(List.of(dto))).get(0);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }

    private Set<Feature> resolveFeatures(Set<Long> featureIds) {
        if (featureIds == null || featureIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(featureRepository.findAllById(featureIds));
    }

    private Set<Policy> resolvePolicies(Set<Long> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(policyRepository.findAllById(policyIds));
    }

    private void replaceImages(Lodging lodging, List<String> imageUrls) {
        lodging.getImages().clear();
        if (imageUrls == null) return;
        imageUrls.forEach(imageUrl -> lodging.getImages().add(LodgingImage.forLodging(lodging, imageUrl)));
    }

    @Override
    @Transactional
    public LodgingDTO save(LodgingDTO dto) {
        if (lodgingRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Ya existe un alojamiento con el nombre: " + dto.getName());
        }
        if (dto.getEmail() != null && lodgingRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un alojamiento con el email: " + dto.getEmail());
        }
        Lodging lodging = dto.toEntity();
        lodging.setCategory(resolveCategory(dto.getCategoryId()));
        lodging.setFeatures(resolveFeatures(dto.getFeatureIds()));
        lodging.setPolicies(resolvePolicies(dto.getPolicyIds()));
        replaceImages(lodging, dto.getImageUrls());

        Lodging saved = lodgingRepository.save(lodging);
        return enrichWithRatings(LodgingDTO.fromEntity(saved));
    }

    @Override
    @Transactional
    public LodgingDTO update(LodgingDTO dto) throws ResourceNotFoundException {
        Lodging lodging = lodgingRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado con ID: " + dto.getId()));
        // Si el email cambió, validar que no esté duplicado
        if (dto.getEmail() != null && !dto.getEmail().equals(lodging.getEmail())
                && lodgingRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un alojamiento con el email: " + dto.getEmail());
        }
        lodging.setName(dto.getName());
        lodging.setDescription(dto.getDescription());
        lodging.setAddress(dto.getAddress());
        lodging.setCity(dto.getCity());
        lodging.setCountry(dto.getCountry());
        lodging.setPhoneNumber(dto.getPhoneNumber());
        lodging.setEmail(dto.getEmail());
        lodging.setPricePerNight(dto.getPricePerNight());
        lodging.setMaxGuests(dto.getMaxGuests());
        lodging.setCategory(resolveCategory(dto.getCategoryId()));
        lodging.setFeatures(resolveFeatures(dto.getFeatureIds()));
        lodging.setPolicies(resolvePolicies(dto.getPolicyIds()));
        replaceImages(lodging, dto.getImageUrls());

        Lodging updated = lodgingRepository.save(lodging);
        return enrichWithRatings(LodgingDTO.fromEntity(updated));
    }

    @Override
    @Transactional
    public Optional<LodgingDTO> delete(Long id) throws ResourceNotFoundException {
        Lodging lodging = lodgingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado con ID: " + id));
        lodgingRepository.deleteById(id);
        return Optional.of(LodgingDTO.fromEntity(lodging));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> findAll() {
        List<LodgingDTO> dtos = lodgingRepository.findAll(PageRequest.of(0, MAX_UNFILTERED_RESULTS))
                .getContent()
                .stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        return enrichWithRatings(dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LodgingDTO> findById(Long id) {
        return lodgingRepository.findById(id).map(l -> enrichWithRatings(LodgingDTO.fromEntity(l)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> findByName(String name) {
        List<LodgingDTO> dtos = lodgingRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        return enrichWithRatings(dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> findByCategory(Long categoryId) {
        List<LodgingDTO> dtos = lodgingRepository.findByCategoryId(categoryId)
                .stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        return enrichWithRatings(dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Lodging> lodgingPage = lodgingRepository.findAll(pageable);
        List<LodgingDTO> dtos = lodgingPage.getContent().stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        List<LodgingDTO> lodgings = enrichWithRatings(dtos);
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Specification<Lodging> spec = adminSearchSpec(query);
        Page<Lodging> lodgingPage = lodgingRepository.findAll(spec, pageable);
        List<LodgingDTO> dtos = lodgingPage.getContent().stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        List<LodgingDTO> lodgings = enrichWithRatings(dtos);

        return new PageResponse<>(
                lodgings,
                lodgingPage.getNumber(),
                lodgingPage.getTotalElements(),
                lodgingPage.getTotalPages()
        );
    }

    private Specification<Lodging> adminSearchSpec(String query) {
        if (query == null || query.isBlank()) {
            return (root, criteriaQuery, cb) -> cb.conjunction();
        }

        String pattern = "%" + query.trim().toLowerCase() + "%";
        return (root, criteriaQuery, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("city")), pattern),
                cb.like(cb.lower(root.get("country")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("address")), pattern)
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
        int currentPage = totalPages == 0 ? 0 : (reset ? 0 : Math.max(0, Math.min(page, totalPages - 1)));
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
            if (lodgings.get(index - 1).getId().compareTo(lodgings.get(index).getId()) > 0) {
                return false;
            }
        }
        return lodgings.size() > 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> findAllRandom() {
        long total = lodgingRepository.count();
        if (total == 0) return List.of();

        // Toma un pool acotado para evitar sobrecargar memoria si la BD crece mucho
        int fetchSize = (int) Math.min(total, RANDOM_POOL_SIZE);
        List<Lodging> pool = new ArrayList<>(lodgingRepository.findAll(PageRequest.of(0, fetchSize)).getContent());
        Collections.shuffle(pool);
        List<LodgingDTO> dtos = pool.stream()
                .limit(RANDOM_RESULT_SIZE)
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        return enrichWithRatings(dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public LodgingSearchResponse search(String city, LocalDate checkIn, LocalDate checkOut,
                                      Integer guests, List<Long> categories,
                                      BigDecimal minPrice, BigDecimal maxPrice,
                                      int page, int size) {
        Specification<Lodging> spec = (root, query, cb) -> cb.conjunction();

        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
        }
        if (guests != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("maxGuests"), guests));
        }
        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("category").get("id").in(categories));
        }
        if (minPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice));
        }

        if (checkIn != null && checkOut != null) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Reservation> r = sub.from(Reservation.class);
                sub.select(r.get("id"))
                   .where(
                       cb.equal(r.get("lodging"), root),
                       cb.equal(r.get("status"), ReservationStatus.CONFIRMED),
                       cb.lessThan(r.<LocalDate>get("checkIn"), checkOut),
                       cb.greaterThan(r.<LocalDate>get("checkOut"), checkIn)
                   );
                return cb.not(cb.exists(sub));
            });
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Lodging> resultsPage = lodgingRepository.findAll(spec, pageable);

        List<LodgingDTO> dtos = resultsPage.getContent().stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        List<LodgingDTO> lodgings = enrichWithRatings(dtos);

        return new LodgingSearchResponse(
                lodgings,
                resultsPage.getNumber(),
                resultsPage.getTotalElements(),
                resultsPage.getTotalPages(),
                lodgingRepository.count()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findCities(String query) {
        String filter = (query == null) ? "" : query;
        return lodgingRepository.findDistinctByCityContainingIgnoreCaseOrderByCityAsc(filter)
                .stream()
                .map(CityProjection::getCity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(Long lodgingId, LocalDate checkIn, LocalDate checkOut) {
        List<Reservation> confirmed = reservationRepository
                .findByLodgingIdAndStatus(lodgingId, ReservationStatus.CONFIRMED);

        List<OccupiedRange> occupiedRanges = confirmed.stream()
                .map(r -> {
                    OccupiedRange range = new OccupiedRange();
                    range.setCheckIn(r.getCheckIn());
                    range.setCheckOut(r.getCheckOut());
                    return range;
                })
                .toList();

        boolean available = checkIn == null || checkOut == null || confirmed.stream()
                .noneMatch(r -> r.getCheckIn().isBefore(checkOut) && r.getCheckOut().isAfter(checkIn));

        AvailabilityResponse response = new AvailabilityResponse();
        response.setAvailable(available);
        response.setOccupiedRanges(occupiedRanges);
        return response;
    }
}
