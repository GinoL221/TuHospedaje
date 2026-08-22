package com.tuhospedaje.controller;

import com.tuhospedaje.dto.rating.RatingDTO;
import com.tuhospedaje.dto.rating.RatingEligibilityDTO;
import com.tuhospedaje.dto.rating.RatingRequest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Ratings", description = "Submit and retrieve lodging ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Submit or update a rating",
            description = "Creates a new rating for a lodging, or updates the existing one if the user " +
                          "has already rated it. Only users with a confirmed reservation for the target " +
                          "lodging are allowed to submit a rating. Score must be between 1 and 5."
    )
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rating submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or user has no confirmed reservation for this lodging", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lodging not found", content = @Content),
    })
    public ResponseEntity<RatingDTO> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RatingRequest request) {
        RatingDTO rating = ratingService.createRating(user, request.getLodgingId(), request.getScore(), request.getComment());
        return ResponseEntity.status(HttpStatus.CREATED).body(rating);
    }

    @GetMapping("/lodging/{lodgingId}")
    @Operation(
            summary = "Get ratings for a lodging",
            description = "Returns all ratings for a given lodging along with the average score and total count. Public endpoint — no authentication required."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ratings retrieved successfully"),
    })
    public ResponseEntity<Map<String, Object>> getByLodging(@PathVariable Long lodgingId) {
        return ResponseEntity.ok(ratingService.getRatingsByLodging(lodgingId));
    }

    @GetMapping("/lodging/{lodgingId}/eligibility")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check rating eligibility",
            description = "Whether the authenticated user has a completed confirmed reservation for this lodging.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eligibility computed successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
    })
    public ResponseEntity<RatingEligibilityDTO> getEligibility(
            @AuthenticationPrincipal User user,
            @PathVariable Long lodgingId) {
        return ResponseEntity.ok(ratingService.getEligibility(user, lodgingId));
    }
}
