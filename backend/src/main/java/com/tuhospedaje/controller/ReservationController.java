package com.tuhospedaje.controller;

import com.tuhospedaje.dto.common.PageResponse;
import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Create and manage lodging reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a reservation",
            description = "Books a lodging for the authenticated user. Returns 400 if the requested " +
                          "dates overlap with an existing confirmed reservation. Returns 409 if a " +
                          "concurrent update conflict is detected (optimistic or pessimistic lock failure)."
    )
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reservation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or date conflict with an existing reservation", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lodging not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Concurrent update conflict — retry the request", content = @Content),
    })
    public ResponseEntity<ReservationResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reservation by ID",
            description = "Returns a reservation by its ID. To prevent IDOR, non-owner users " +
                          "receive a 404 instead of 403 — the resource existence is not disclosed."
    )
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation found"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reservation not found or not owned by the requester", content = @Content),
    })
    public ResponseEntity<ReservationResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservationService.getReservationById(id, user));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel an owned reservation")
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation cancelled or already cancelled"),
            @ApiResponse(responseCode = "400", description = "Cancellation deadline passed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reservation not found or not owned", content = @Content),
            @ApiResponse(responseCode = "409", description = "Concurrent update conflict", content = @Content)
    })
    public ResponseEntity<ReservationResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservationService.cancelReservation(id, user));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "List my reservations",
            description = "Returns all reservations belonging to the authenticated user, ordered by check-in date descending."
    )
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
    })
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservationService.getMyReservations(user));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "List reservations for Admin",
            description = "Returns server-paginated reservation rows for the Admin table."
    )
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin reservation page returned"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
    })
    public ResponseEntity<PageResponse<ReservationResponse>> getAdminReservations(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{error.page.negative}") int page,
            @RequestParam(defaultValue = "10") @Max(value = 100, message = "{error.size.max}") @Min(value = 1, message = "{error.size.negative}") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(reservationService.getAdminReservations(page, size, sort, direction, status, q));
    }
}
