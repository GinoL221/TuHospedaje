package com.tuhospedaje.controller;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.service.UserService;
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
public class FavoriteController {

    private final UserService userService;

    public FavoriteController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{lodgingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> addFavorite(@AuthenticationPrincipal User user, @PathVariable Long lodgingId) {
        userService.addFavorite(user.getId(), lodgingId);
        return ResponseEntity.ok("Favorito agregado");
    }

    @DeleteMapping("/{lodgingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> removeFavorite(@AuthenticationPrincipal User user, @PathVariable Long lodgingId) {
        userService.removeFavorite(user.getId(), lodgingId);
        return ResponseEntity.ok("Favorito eliminado");
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LodgingDTO>> getFavorites(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getFavorites(user.getId()));
    }
}
