package com.tuhospedaje.controller;

import com.tuhospedaje.dto.policy.PolicyDTO;
import com.tuhospedaje.service.PolicyService;
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
@RequestMapping("/api/policies")
@Tag(name = "Policies", description = "Manage lodging house rules and policies (e.g. No Smoking, Pets Allowed)")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    @Operation(summary = "List all policies", description = "Returns all available house rules and policies.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policies retrieved successfully"),
    })
    public ResponseEntity<List<PolicyDTO>> findAll() {
        return ResponseEntity.ok(policyService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get policy by ID", description = "Returns a single policy by its unique identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy found"),
            @ApiResponse(responseCode = "404", description = "Policy not found", content = @Content),
    })
    public ResponseEntity<PolicyDTO> findById(@PathVariable Long id) {
        return policyService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a policy", description = "Creates a new house rule or policy. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Policy created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
    })
    public ResponseEntity<PolicyDTO> create(@Valid @RequestBody PolicyDTO dto) {
        PolicyDTO saved = policyService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a policy", description = "Updates an existing policy by ID. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Policy not found", content = @Content),
    })
    public ResponseEntity<PolicyDTO> update(@PathVariable Long id, @Valid @RequestBody PolicyDTO dto) {
        dto.setId(id);
        PolicyDTO updated = policyService.update(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a policy", description = "Permanently deletes a policy by ID. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Policy deleted successfully", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "Policy not found", content = @Content),
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
