package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.reservation.AvailabilityResponse;
import com.tuhospedaje.dto.reservation.OccupiedRange;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.entity.Rating;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class LodgingServiceImpl implements LodgingService {

    private static final int RANDOM_POOL_SIZE = 100;
    private static final int RANDOM_RESULT_SIZE = 10;

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

    private LodgingDTO enrichWithRatings(LodgingDTO dto) {
        List<Rating> ratings = ratingRepository.findByLodgingIdOrderByCreatedAtDesc(dto.getId());
        int count = ratings.size();
        double avg = ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);
        dto.setRatingCount(count);
        dto.setAverageRating(Math.round(avg * 10.0) / 10.0);
        return dto;
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
        lodging.setCategory(resolveCategory(dto.getCategoryId()));
        lodging.setFeatures(resolveFeatures(dto.getFeatureIds()));
        lodging.setPolicies(resolvePolicies(dto.getPolicyIds()));

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
        return lodgingRepository.findAll()
                .stream()
                .map(l -> enrichWithRatings(LodgingDTO.fromEntity(l)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LodgingDTO> findById(Long id) {
        return lodgingRepository.findById(id).map(l -> enrichWithRatings(LodgingDTO.fromEntity(l)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> findByName(String name) {
        return lodgingRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(l -> enrichWithRatings(LodgingDTO.fromEntity(l)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> findByCategory(Long categoryId) {
        return lodgingRepository.findByCategoryId(categoryId)
                .stream()
                .map(l -> enrichWithRatings(LodgingDTO.fromEntity(l)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Lodging> lodgingPage = lodgingRepository.findAll(pageable);
        List<LodgingDTO> lodgings = lodgingPage.getContent().stream()
                .map(l -> enrichWithRatings(LodgingDTO.fromEntity(l)))
                .toList();
        Map<String, Object> response = new HashMap<>();
        response.put("lodgings", lodgings);
        response.put("currentPage", lodgingPage.getNumber());
        response.put("totalItems", lodgingPage.getTotalElements());
        response.put("totalPages", lodgingPage.getTotalPages());
        return response;
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
        return pool.stream()
                .limit(RANDOM_RESULT_SIZE)
                .map(l -> enrichWithRatings(LodgingDTO.fromEntity(l)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> search(String city, LocalDate checkIn, LocalDate checkOut,
                                   Integer guests, Long category,
                                   BigDecimal minPrice, BigDecimal maxPrice) {
        Specification<Lodging> spec = (root, query, cb) -> cb.conjunction();

        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
        }
        if (guests != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("maxGuests"), guests));
        }
        if (category != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), category));
        }
        if (minPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice));
        }

        List<Lodging> results = lodgingRepository.findAll(spec);

        if (checkIn != null && checkOut != null) {
            results = results.stream()
                    .filter(l -> reservationRepository
                            .findByLodgingIdAndStatus(l.getId(), ReservationStatus.CONFIRMED)
                            .stream()
                            .noneMatch(r -> r.getCheckIn().isBefore(checkOut)
                                    && r.getCheckOut().isAfter(checkIn)))
                    .toList();
        }

        return results.stream()
                .map(l -> enrichWithRatings(LodgingDTO.fromEntity(l)))
                .toList();
    }

    @Override
    public List<String> findCities(String query) {
        String filter = (query == null) ? "" : query;
        return lodgingRepository.findDistinctByCityContainingIgnoreCaseOrderByCityAsc(filter)
                .stream()
                .map(CityProjection::getCity)
                .toList();
    }

    @Override
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
