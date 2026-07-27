package com.tuhospedaje.controller;

import com.tuhospedaje.dto.user.UserDTO;
import com.tuhospedaje.dto.auth.RoleRequest;
import com.tuhospedaje.dto.auth.UserStatusRequest;
import com.tuhospedaje.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management — requires ADMIN role")
@SecurityRequirement(name = "csrfToken")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List all users", description = "Returns the complete list of registered users. Restricted to ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Update user role", description = "Assigns a new role to the specified user. Restricted to ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error — role field is missing or blank", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found for the given ID", content = @Content)
    })
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateRole(
            @Parameter(description = "ID of the user whose role will be updated", example = "1") @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {
        UserDTO updated = userService.updateRole(id, request.getRole());
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Enable or disable a user account", description = "Flips the account's enabled "
            + "flag. Disabling immediately revokes all of that user's refresh sessions and, combined with "
            + "the JwtAuthenticationFilter enabled-check, rejects their very next authenticated request "
            + "even if their access token has not yet expired. Restricted to ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account status updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found for the given ID", content = @Content)
    })
    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> setEnabled(
            @Parameter(description = "ID of the user whose enabled status will change", example = "1") @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        UserDTO updated = userService.setEnabled(id, request.isEnabled());
        return ResponseEntity.ok(updated);
    }
}
