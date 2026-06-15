package com.tuhospedaje.controller;

import com.tuhospedaje.dto.features.FeatureDTO;
import com.tuhospedaje.service.FeatureService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/features")
@Tag(name = "Features", description = "Manage lodging amenity features (e.g. Wi-Fi, Pool, Parking)")
public class FeatureController {

    private final FeatureService featureService;

    public FeatureController(FeatureService featureService) {
        this.featureService = featureService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a feature", description = "Creates a new amenity feature. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Feature created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
    })
    public ResponseEntity<FeatureDTO> create(@Valid @RequestBody FeatureDTO dto) {
        FeatureDTO saved = featureService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    @Operation(summary = "List all features", description = "Returns all available amenity features.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Features retrieved successfully"),
    })
    public ResponseEntity<List<FeatureDTO>> findAll() {
        return ResponseEntity.ok(featureService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get feature by ID", description = "Returns a single amenity feature by its unique identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feature found"),
            @ApiResponse(responseCode = "404", description = "Feature not found", content = @Content),
    })
    public ResponseEntity<FeatureDTO> findById(@PathVariable Long id) {
        return featureService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a feature", description = "Updates an existing amenity feature by ID. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feature updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Feature not found", content = @Content),
    })
    public ResponseEntity<FeatureDTO> update(@PathVariable Long id, @Valid @RequestBody FeatureDTO dto) {
        dto.setId(id);
        FeatureDTO updated = featureService.update(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a feature", description = "Permanently deletes an amenity feature by ID. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Feature deleted successfully", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Feature not found", content = @Content),
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        featureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
