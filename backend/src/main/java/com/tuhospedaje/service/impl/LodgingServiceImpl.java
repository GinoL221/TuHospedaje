package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.common.PageResponse;
import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.lodging.LodgingSearchResponse;
import com.tuhospedaje.dto.lodging.RecommendationPageResponse;
import com.tuhospedaje.dto.reservation.AvailabilityResponse;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Feature;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.LodgingImage;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.PolicyRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.service.LodgingQueryService;
import com.tuhospedaje.service.LodgingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LodgingServiceImpl implements LodgingService {

    private final LodgingRepository lodgingRepository;
    private final CategoryRepository categoryRepository;
    private final FeatureRepository featureRepository;
    private final PolicyRepository policyRepository;
    private final RatingRepository ratingRepository;
    private final LodgingQueryService lodgingQueryService;

    public LodgingServiceImpl(LodgingRepository lodgingRepository, CategoryRepository categoryRepository,
                              FeatureRepository featureRepository, PolicyRepository policyRepository,
                              RatingRepository ratingRepository, LodgingQueryService lodgingQueryService) {
        this.lodgingRepository = lodgingRepository;
        this.categoryRepository = categoryRepository;
        this.featureRepository = featureRepository;
        this.policyRepository = policyRepository;
        this.ratingRepository = ratingRepository;
        this.lodgingQueryService = lodgingQueryService;
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

    private LodgingDTO enrichWithRatings(LodgingDTO dto) {
        return enrichWithRatings(new ArrayList<>(List.of(dto))).get(0);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
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
        return enrichWithRatings(LodgingDTO.fromEntity(lodgingRepository.save(lodging)));
    }

    @Override
    @Transactional
    public LodgingDTO update(LodgingDTO dto) throws ResourceNotFoundException {
        Lodging lodging = lodgingRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado con ID: " + dto.getId()));
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
        return enrichWithRatings(LodgingDTO.fromEntity(lodgingRepository.save(lodging)));
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
    public Optional<LodgingDTO> findById(Long id) {
        return lodgingRepository.findById(id).map(lodging -> enrichWithRatings(LodgingDTO.fromEntity(lodging)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LodgingDTO> findByName(String name) {
        List<LodgingDTO> dtos = lodgingRepository.findByNameContainingIgnoreCase(name).stream()
                .map(LodgingDTO::fromEntity)
                .collect(Collectors.toList());
        return enrichWithRatings(dtos);
    }

    @Override
    public List<LodgingDTO> findAll() {
        return lodgingQueryService.findAll();
    }

    @Override
    public List<LodgingDTO> findByCategory(Long categoryId) {
        return lodgingQueryService.findByCategory(categoryId);
    }

    @Override
    public Map<String, Object> findAllPaginated(int page, int size) {
        return lodgingQueryService.findAllPaginated(page, size);
    }

    @Override
    public PageResponse<LodgingDTO> findAdminPage(int page, int size, String sort, String direction, String query) {
        return lodgingQueryService.findAdminPage(page, size, sort, direction, query);
    }

    @Override
    public List<LodgingDTO> findAllRandom() {
        return lodgingQueryService.findAllRandom();
    }

    @Override
    public RecommendationPageResponse findRecommendations(String seed, int page, int size, String revision) {
        return lodgingQueryService.findRecommendations(seed, page, size, revision);
    }

    @Override
    public LodgingSearchResponse search(String city, LocalDate checkIn, LocalDate checkOut, Integer guests,
                                        List<Long> categories, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        return lodgingQueryService.search(city, checkIn, checkOut, guests, categories, minPrice, maxPrice, page, size);
    }

    @Override
    public List<String> findCities(String query) {
        return lodgingQueryService.findCities(query);
    }

    @Override
    public AvailabilityResponse checkAvailability(Long lodgingId, LocalDate checkIn, LocalDate checkOut) {
        return lodgingQueryService.checkAvailability(lodgingId, checkIn, checkOut);
    }
}
