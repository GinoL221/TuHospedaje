package com.tuhospedaje.controller;

import com.tuhospedaje.dto.admin.AdminStatsResponse;
import com.tuhospedaje.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Aggregate figures for the admin dashboard — ADMIN only")
public class AdminController {

    private final AdminStatsService adminStatsService;

    public AdminController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Row counts for the admin dashboard",
            description = "Returns how many lodgings, categories, features, users and reservations exist. "
                          + "Counting happens in the database; no listing payload is transferred, and no "
                          + "count inherits a listing's page or result cap."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Counts returned"),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
    })
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminStatsService.collect());
    }
}
