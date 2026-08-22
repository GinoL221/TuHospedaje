package com.tuhospedaje.service;

import com.tuhospedaje.dto.rating.RatingDTO;
import com.tuhospedaje.dto.rating.RatingEligibilityDTO;
import com.tuhospedaje.entity.User;

import java.util.Map;

public interface RatingService {

    RatingDTO createRating(User user, Long lodgingId, Integer score, String comment);

    Map<String, Object> getRatingsByLodging(Long lodgingId);

    RatingEligibilityDTO getEligibility(User user, Long lodgingId);
}