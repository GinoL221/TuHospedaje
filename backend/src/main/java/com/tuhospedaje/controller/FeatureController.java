package com.tuhospedaje.controller;

import com.tuhospedaje.dto.features.FeatureDTO;
import com.tuhospedaje.service.FeatureService;
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
public class FeatureController {

    private final FeatureService featureService;

    public FeatureController(FeatureService featureService) {
        this.featureService = featureService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeatureDTO> create(@Valid @RequestBody FeatureDTO dto) {
        FeatureDTO saved = featureService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    @GetMapping
    public ResponseEntity<List<FeatureDTO>> findAll() {
        return ResponseEntity.ok(featureService.findAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<FeatureDTO> findById(@PathVariable Long id) {
        return featureService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeatureDTO> update(@PathVariable Long id, @Valid @RequestBody FeatureDTO dto) {
        dto.setId(id);
        FeatureDTO updated = featureService.update(dto);
        return ResponseEntity.ok(updated);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        featureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
