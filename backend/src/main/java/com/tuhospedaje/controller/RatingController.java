package com.tuhospedaje.controller;

import com.tuhospedaje.dto.rating.RatingDTO;
import com.tuhospedaje.dto.rating.RatingRequest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RatingDTO> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RatingRequest request) {
        RatingDTO rating = ratingService.createRating(user, request.getLodgingId(), request.getScore(), request.getComment());
        return ResponseEntity.status(HttpStatus.CREATED).body(rating);
    }

    @GetMapping("/lodging/{lodgingId}")
    public ResponseEntity<Map<String, Object>> getByLodging(@PathVariable Long lodgingId) {
        return ResponseEntity.ok(ratingService.getRatingsByLodging(lodgingId));
    }
}
