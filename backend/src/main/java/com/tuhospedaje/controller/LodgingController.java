package com.tuhospedaje.controller;

import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.dto.reservation.AvailabilityResponse;
import com.tuhospedaje.service.LodgingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lodgings")
public class LodgingController {

    private final LodgingService lodgingService;

    public LodgingController(LodgingService lodgingService) {
        this.lodgingService = lodgingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LodgingDTO> create(@Valid @RequestBody LodgingDTO dto) {
        LodgingDTO saved = lodgingService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LodgingDTO> update(@PathVariable Long id, @RequestBody LodgingDTO dto) {
        dto.setId(id);
        LodgingDTO updated = lodgingService.update(dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
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

    @GetMapping("/random")
    public ResponseEntity<List<LodgingDTO>> random() {
        return ResponseEntity.ok(lodgingService.findAllRandom());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LodgingDTO> findById(@PathVariable Long id) {
        return lodgingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        lodgingService.delete(id);
        return ResponseEntity.ok("Alojamiento eliminado con ID: " + id);
    }

    @GetMapping("/search")
    public ResponseEntity<List<LodgingDTO>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok(lodgingService.search(city, checkIn, checkOut, guests, category, minPrice, maxPrice));
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> cities(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(lodgingService.findCities(q));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> availability(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(lodgingService.checkAvailability(id, checkIn, checkOut));
    }
}
