package com.tuhospedaje.controller;

import com.tuhospedaje.dto.UserDTO;
import com.tuhospedaje.dto.auth.RoleRequest;
import com.tuhospedaje.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        UserDTO updated = userService.updateRole(id, request.getRole());
        return ResponseEntity.ok(updated);
    }
}
