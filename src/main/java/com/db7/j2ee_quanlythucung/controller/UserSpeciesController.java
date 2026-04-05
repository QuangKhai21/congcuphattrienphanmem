package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.UserBreed;
import com.db7.j2ee_quanlythucung.entity.UserSpecies;
import com.db7.j2ee_quanlythucung.repository.UserBreedRepository;
import com.db7.j2ee_quanlythucung.repository.UserSpeciesRepository;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-species")
@RequiredArgsConstructor
public class UserSpeciesController {

    private final UserSpeciesRepository speciesRepository;
    private final UserBreedRepository breedRepository;

    // ── Species CRUD ──────────────────────────────────────────────

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> listSpecies(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        Long userId = userDetails.getUser().getId();
        List<UserSpecies> species = speciesRepository.findByOwnerIdOrderByNameAsc(userId);
        List<Map<String, Object>> result = species.stream().map(s -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("name", s.getName());
            item.put("icon", s.getIcon());
            item.put("imageUrl", s.getImageUrl());
            item.put("description", s.getDescription());
            // 使用breedRepository单独查询来避免懒加载问题
            item.put("breedCount", breedRepository.countBySpeciesId(s.getId()));
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createSpecies(
            @RequestParam String name,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tên loài không được trống"));
        }
        Long userId = userDetails.getUser().getId();
        if (speciesRepository.existsByNameIgnoreCaseAndOwnerId(name.trim(), userId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Loài này đã tồn tại"));
        }

        UserSpecies species = UserSpecies.builder()
                .name(name.trim())
                .icon(icon)
                .imageUrl(imageUrl)
                .description(description)
                .owner(userDetails.getUser())
                .build();
        species = speciesRepository.save(species);

        Map<String, Object> result = new HashMap<>();
        result.put("id", species.getId());
        result.put("name", species.getName());
        result.put("icon", species.getIcon());
        result.put("imageUrl", species.getImageUrl());
        result.put("description", species.getDescription());
        result.put("breedCount", 0);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateSpecies(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        UserSpecies species = speciesRepository.findByIdAndOwnerId(id, userDetails.getUser().getId())
                .orElse(null);
        if (species == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy loài"));
        }

        species.setName(name.trim());
        species.setIcon(icon);
        species.setImageUrl(imageUrl);
        species.setDescription(description);
        speciesRepository.save(species);

        Map<String, Object> result = new HashMap<>();
        result.put("id", species.getId());
        result.put("name", species.getName());
        result.put("icon", species.getIcon());
        result.put("imageUrl", species.getImageUrl());
        result.put("description", species.getDescription());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteSpecies(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        UserSpecies species = speciesRepository.findByIdAndOwnerId(id, userDetails.getUser().getId())
                .orElse(null);
        if (species == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy loài"));
        }
        speciesRepository.delete(species);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Breed CRUD ─────────────────────────────────────────────────

    @GetMapping("/{speciesId}/breeds")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listBreeds(@PathVariable Long speciesId,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        // Verify ownership
        UserSpecies species = speciesRepository.findByIdAndOwnerId(speciesId, userDetails.getUser().getId())
                .orElse(null);
        if (species == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy loài"));
        }
        List<UserBreed> breeds = breedRepository.findBySpeciesIdOrderByNameAsc(speciesId);
        List<Map<String, Object>> result = breeds.stream().map(b -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", b.getId());
            item.put("name", b.getName());
            item.put("imageUrl", b.getImageUrl());
            item.put("description", b.getDescription());
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{speciesId}/breeds")
    @Transactional
    public ResponseEntity<?> createBreed(
            @PathVariable Long speciesId,
            @RequestParam String name,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        UserSpecies species = speciesRepository.findByIdAndOwnerId(speciesId, userDetails.getUser().getId())
                .orElse(null);
        if (species == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy loài"));
        }
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tên giống không được trống"));
        }

        UserBreed breed = UserBreed.builder()
                .name(name.trim())
                .imageUrl(imageUrl)
                .description(description)
                .species(species)
                .build();
        breed = breedRepository.save(breed);

        Map<String, Object> result = new HashMap<>();
        result.put("id", breed.getId());
        result.put("name", breed.getName());
        result.put("imageUrl", breed.getImageUrl());
        result.put("description", breed.getDescription());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{speciesId}/breeds/{breedId}")
    @Transactional
    public ResponseEntity<?> deleteBreed(
            @PathVariable Long speciesId,
            @PathVariable Long breedId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        UserSpecies species = speciesRepository.findByIdAndOwnerId(speciesId, userDetails.getUser().getId())
                .orElse(null);
        if (species == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy loài"));
        }
        breedRepository.deleteById(breedId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}