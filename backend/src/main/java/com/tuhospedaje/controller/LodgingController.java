package com.tuhospedaje.controller;

import com.tuhospedaje.dto.LodgingDTO;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.LodgingImage;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.service.ILodgingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lodgings")
public class LodgingController {

    private final ILodgingService lodgingService;

    public LodgingController(ILodgingService lodgingService) {
        this.lodgingService = lodgingService;
    }

    @PostMapping
    public ResponseEntity<LodgingDTO> create(@RequestBody LodgingDTO dto) {
        if (dto.getId() != null) {
            return ResponseEntity.badRequest().build();
        }
        LodgingDTO saved = lodgingService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LodgingDTO> update(@PathVariable Long id, @RequestBody LodgingDTO dto) {
        dto.setId(id);
        LodgingDTO updated = lodgingService.update(dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<?> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
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

    @GetMapping("/search")
    public ResponseEntity<List<LodgingDTO>> search(@RequestParam String query) {
        return ResponseEntity.ok(lodgingService.findByName(query));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        lodgingService.delete(id);
        return ResponseEntity.ok("Alojamiento eliminado con ID: " + id);
    }
}
