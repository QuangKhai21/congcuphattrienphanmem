package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.UserBreed;
import com.db7.j2ee_quanlythucung.entity.UserSpecies;
import com.db7.j2ee_quanlythucung.repository.UserBreedRepository;
import com.db7.j2ee_quanlythucung.repository.UserSpeciesRepository;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/my-species")
@RequiredArgsConstructor
public class MySpeciesController {

    private final UserSpeciesRepository speciesRepository;
    private final UserBreedRepository breedRepository;

    @GetMapping
    public String mySpecies(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        return "user/my-species";
    }

    // ── Trang Thêm loài (như form thú cưng) ────────────────────────
    @GetMapping("/add")
    public String addSpeciesForm(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                  Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        model.addAttribute("species", new UserSpecies());
        return "user/species-form";
    }

    // ── Trang Sửa loài ─────────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String editSpeciesForm(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetailsImpl userDetails,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        UserSpecies species = speciesRepository.findByIdAndOwnerId(id, userDetails.getUser().getId())
                .orElse(null);
        if (species == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy loài!");
            return "redirect:/my-species";
        }
        model.addAttribute("species", species);
        return "user/species-form";
    }

    // ── Lưu loài (thêm mới hoặc cập nhật) ──────────────────────────
    @PostMapping("/save")
    @Transactional
    public String saveSpecies(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<String> breedNames,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Tên loài không được trống!");
            if (id != null) {
                return "redirect:/my-species/" + id + "/edit";
            }
            return "redirect:/my-species/add";
        }

        if (id != null) {
            // Cập nhật
            UserSpecies species = speciesRepository.findByIdAndOwnerId(id, userDetails.getUser().getId())
                    .orElse(null);
            if (species == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy loài!");
                return "redirect:/my-species";
            }
            species.setName(name.trim());
            if (icon != null) {
                species.setIcon(icon.isBlank() ? null : icon.trim());
            }
            if (imageUrl != null) {
                species.setImageUrl(imageUrl.isBlank() ? null : imageUrl.trim());
            }
            species.setDescription(description);
            speciesRepository.save(species);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật loài: " + species.getName());
        } else {
            // Thêm mới
            UserSpecies species = UserSpecies.builder()
                    .name(name.trim())
                    .icon(icon != null && !icon.isBlank() ? icon.trim() : null)
                    .imageUrl(imageUrl != null && !imageUrl.isBlank() ? imageUrl.trim() : null)
                    .description(description)
                    .owner(userDetails.getUser())
                    .build();
            species = speciesRepository.save(species);
            if (breedNames != null) {
                for (String bn : breedNames) {
                    if (bn == null || bn.isBlank()) {
                        continue;
                    }
                    UserBreed breed = UserBreed.builder()
                            .name(bn.trim())
                            .species(species)
                            .build();
                    breedRepository.save(breed);
                }
            }
            redirectAttributes.addFlashAttribute("success", "Đã thêm loài mới: " + species.getName());
        }

        return "redirect:/my-species";
    }

    // ── Xóa loài ────────────────────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String deleteSpecies(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetailsImpl userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        UserSpecies species = speciesRepository.findByIdAndOwnerId(id, userDetails.getUser().getId())
                .orElse(null);
        if (species == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy loài!");
            return "redirect:/my-species";
        }
        speciesRepository.delete(species);
        redirectAttributes.addFlashAttribute("success", "Đã xóa loài!");
        return "redirect:/my-species";
    }
}
