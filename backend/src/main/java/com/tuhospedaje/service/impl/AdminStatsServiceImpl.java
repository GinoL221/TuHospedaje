package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.admin.AdminStatsResponse;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.FeatureRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.AdminStatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStatsServiceImpl implements AdminStatsService {

    private final LodgingRepository lodgingRepository;
    private final CategoryRepository categoryRepository;
    private final FeatureRepository featureRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public AdminStatsServiceImpl(LodgingRepository lodgingRepository,
                                 CategoryRepository categoryRepository,
                                 FeatureRepository featureRepository,
                                 UserRepository userRepository,
                                 ReservationRepository reservationRepository) {
        this.lodgingRepository = lodgingRepository;
        this.categoryRepository = categoryRepository;
        this.featureRepository = featureRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Five {@code SELECT COUNT(*)} in one read-only transaction, so the cards read from a
     * single consistent snapshot instead of five independently-timed requests.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse collect() {
        return new AdminStatsResponse(
                lodgingRepository.count(),
                categoryRepository.count(),
                featureRepository.count(),
                userRepository.count(),
                reservationRepository.count()
        );
    }
}
