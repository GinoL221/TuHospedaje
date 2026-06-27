package com.tuhospedaje.controller;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "Favorites", description = "Manage the authenticated user's favorite lodgings")
public class FavoriteController {

    private final UserService userService;

    public FavoriteController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{lodgingId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Add a lodging to favorites",
            description = "Marks the specified lodging as a favorite for the authenticated user."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lodging added to favorites", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lodging not found", content = @Content),
    })
    public ResponseEntity<Void> addFavorite(@AuthenticationPrincipal User user, @PathVariable Long lodgingId) {
        userService.addFavorite(user.getId(), lodgingId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{lodgingId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Remove a lodging from favorites",
            description = "Removes the specified lodging from the authenticated user's favorites list."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lodging removed from favorites", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lodging not found", content = @Content),
    })
    public ResponseEntity<Void> removeFavorite(@AuthenticationPrincipal User user, @PathVariable Long lodgingId) {
        userService.removeFavorite(user.getId(), lodgingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "List favorite lodgings",
            description = "Returns the full list of lodgings the authenticated user has marked as favorites."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite lodgings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
    })
    public ResponseEntity<List<LodgingDTO>> getFavorites(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getFavorites(user.getId()));
    }
}
