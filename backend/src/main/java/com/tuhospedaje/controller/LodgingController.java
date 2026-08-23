package com.tuhospedaje.controller;

import com.tuhospedaje.dto.common.PageResponse;
import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.lodging.RecommendationPageResponse;
import com.tuhospedaje.dto.reservation.AvailabilityResponse;
import com.tuhospedaje.service.LodgingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lodgings")
@Tag(name = "Lodgings", description = "Search and manage lodging listings")
@Validated
public class LodgingController {

    private final LodgingService lodgingService;

    public LodgingController(LodgingService lodgingService) {
        this.lodgingService = lodgingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a lodging", description = "Creates a new lodging listing. Requires ADMIN role.")
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lodging created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
    })
    public ResponseEntity<LodgingDTO> create(@Valid @RequestBody LodgingDTO dto) {
        LodgingDTO saved = lodgingService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a lodging", description = "Updates an existing lodging by ID. Requires ADMIN role.")
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lodging updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lodging not found", content = @Content),
    })
    public ResponseEntity<LodgingDTO> update(@PathVariable Long id, @Valid @RequestBody LodgingDTO dto) {
        dto.setId(id);
        LodgingDTO updated = lodgingService.update(dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    @Operation(summary = "List lodgings", description = "Returns all lodgings. Supports optional pagination (?page=0&size=10) and category filtering (?category=1).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lodgings retrieved successfully"),
    })
    public ResponseEntity<?> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long category) {
        if (category != null) {
            return ResponseEntity.ok(lodgingService.findByCategory(category));
        }
        if (page != null && size != null) {
            return ResponseEntity.ok(lodgingService.findAllPaginated(page, size));
        }
        return ResponseEntity.ok(lodgingService.findAll());
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get stable random recommendations", description = "Returns a deterministic recommendation page for a client seed and catalog revision.")
    public ResponseEntity<RecommendationPageResponse> recommendations(
            @RequestParam @Pattern(regexp = "[A-Za-z0-9_-]{16,64}") String seed,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{error.page.negative}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{error.size.negative}") @Max(value = 10, message = "{error.size.max}") int size,
            @RequestParam(required = false) String revision) {
        return ResponseEntity.ok(lodgingService.findRecommendations(seed, page, size, revision));
    }

    @GetMapping("/random")
    @Operation(summary = "Get random lodgings", description = "Returns a random sample of lodging listings for discovery purposes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Random lodgings retrieved successfully"),
    })
    public ResponseEntity<List<LodgingDTO>> random() {
        return ResponseEntity.ok(lodgingService.findAllRandom());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List lodgings for Admin", description = "Returns server-paginated lodging rows for the Admin table.")
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin lodging page returned"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
    })
    public ResponseEntity<PageResponse<LodgingDTO>> adminPage(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{error.page.negative}") int page,
            @RequestParam(defaultValue = "10") @Max(value = 100, message = "{error.size.max}") @Min(value = 1, message = "{error.size.negative}") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(lodgingService.findAdminPage(page, size, sort, direction, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lodging by ID", description = "Returns a single lodging by its unique identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lodging found"),
            @ApiResponse(responseCode = "404", description = "Lodging not found", content = @Content),
    })
    public ResponseEntity<LodgingDTO> findById(@PathVariable Long id) {
        return lodgingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a lodging", description = "Permanently deletes a lodging by ID. Requires ADMIN role.")
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lodging deleted successfully", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lodging not found", content = @Content),
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lodgingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search lodgings", description = "Filters lodgings by city, check-in/check-out dates, number of guests, one or more categories, and price range, with pagination (?page=0&size=9 by default). All filter parameters are optional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned as a paginated wrapper ({lodgings, currentPage, totalItems, totalPages})"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters (e.g. negative page)", content = @Content),
    })
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{error.page.negative}") int page,
            @RequestParam(defaultValue = "9") @Min(value = 1, message = "{error.size.negative}") int size) {
        return ResponseEntity.ok(lodgingService.search(
                city, checkIn, checkOut, guests, categories, minPrice, maxPrice, page, size));
    }

    @GetMapping("/cities")
    @Operation(summary = "List available cities", description = "Returns a list of cities that have at least one lodging. Supports optional query string filtering (?q=bue).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "City list returned"),
    })
    public ResponseEntity<List<String>> cities(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(lodgingService.findCities(q));
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Check lodging availability", description = "Returns availability status for a lodging in the given date range.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability information returned"),
            @ApiResponse(responseCode = "404", description = "Lodging not found", content = @Content),
    })
    public ResponseEntity<AvailabilityResponse> availability(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(lodgingService.checkAvailability(id, checkIn, checkOut));
    }
}
