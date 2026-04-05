package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.*;
import com.db7.j2ee_quanlythucung.entity.Pet.PetStatus;
import com.db7.j2ee_quanlythucung.repository.*;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.HealthService;
import com.db7.j2ee_quanlythucung.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final HealthService healthService;
    private final PetCategoryRepository petCategoryRepository;
    private final BreedRepository breedRepository;
    private final UserRepository userRepository;
    private final UserSpeciesRepository userSpeciesRepository;
    private final UserBreedRepository userBreedRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /** Tránh client gửi id=0 / pet.id giả mạo khiến persist lỗi hoặc gán sai thú cưng. */
    @InitBinder("weightRecord")
    public void initWeightRecordBinder(WebDataBinder binder) {
        binder.setDisallowedFields("id", "pet", "recordedBy", "createdAt");
    }

    private String saveAvatar(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        Path uploadPath = Paths.get(uploadDir, "pet-avatars");
        Files.createDirectories(uploadPath);
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), uploadPath.resolve(filename));
        return "/uploads/pet-avatars/" + filename;
    }

    // ==================== Kiểm tra quyền truy cập ====================

    private boolean isAdminOrManager(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().equals(Role.RoleType.ROLE_ADMIN) || r.getName().equals(Role.RoleType.ROLE_MANAGER));
    }

    private Pet checkPetAccess(Long petId, UserDetailsImpl userDetails) {
        Pet pet = petService.findById(petId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay thu cung"));
        User currentUser = userDetails.getUser();
        if (!isAdminOrManager(currentUser) && !pet.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("Ban khong co quyen truy cap thu cung nay");
        }
        return pet;
    }

    // ==================== Danh sách thú cưng ====================

    @GetMapping
    public String listPets(@AuthenticationPrincipal UserDetailsImpl userDetails,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long categoryId,
                          @RequestParam(required = false) String sort,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userDetails.getUser();
        page = Math.max(0, page);

        int pageSize = 12;
        Sort sortOrder = "name_desc".equals(sort)
                ? Sort.by("name").descending()
                : Sort.by("name").ascending();
        Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

        Page<Pet> petPage;
        if (isAdminOrManager(user)) {
            petPage = petService.search(keyword, categoryId, pageable);
        } else {
            petPage = petService.searchByOwner(keyword, categoryId, user.getId(), pageable);
        }

        model.addAttribute("pets", petPage.getContent());
        model.addAttribute("totalPages", petPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalItems", petPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", petCategoryRepository.findAll());

        return "pets/list";
    }

    // ==================== Form thêm thú cưng ====================

    @GetMapping("/new")
    public String showCreateForm(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        model.addAttribute("pet", new Pet());
        model.addAttribute("categories", petCategoryRepository.findAll());
        model.addAttribute("breeds", breedRepository.findAll());
        model.addAttribute("userSpecies", userSpeciesRepository.findByOwnerIdOrderByNameAsc(userDetails.getUser().getId()));
        return "pets/form";
    }

    // ==================== Xử lý thêm thú cưng ====================

    @PostMapping("/create")
    @Transactional
    public String createPet(@Valid @ModelAttribute Pet pet,
                           BindingResult result,
                           @AuthenticationPrincipal UserDetailsImpl userDetails,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) Long userSpeciesId,
                           @RequestParam(required = false) Long breedId,
                           @RequestParam(required = false) Long userBreedId,
                           @RequestParam(required = false) MultipartFile avatarFile,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            User owner = userDetails.getUser();

            // Ưu tiên loài tự tạo, không thì dùng hệ thống
            if (userSpeciesId != null && userSpeciesId > 0) {
                UserSpecies us = userSpeciesRepository.findByIdAndOwnerId(userSpeciesId, owner.getId()).orElse(null);
                if (us == null) throw new IllegalArgumentException("Khong tim thay loai tu tao");
                pet.setUserSpecies(us);
                pet.setCategory(null); // không dùng category hệ thống

                if (userBreedId != null && userBreedId > 0) {
                    pet.setUserBreed(userBreedRepository.findById(userBreedId).orElse(null));
                } else {
                    pet.setUserBreed(null);
                }
            } else if (categoryId != null && categoryId > 0) {
                PetCategory category = petCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Khong tim thay loai thu cung"));
                pet.setCategory(category);
                pet.setUserSpecies(null);

                if (breedId != null && breedId > 0) {
                    pet.setBreed(breedRepository.findById(breedId).orElse(null));
                } else {
                    pet.setBreed(null);
                }
                pet.setUserBreed(null);
            } else {
                throw new IllegalArgumentException("Vui long chon loai thu cung");
            }

            if (result.hasErrors()) {
                model.addAttribute("categories", petCategoryRepository.findAll());
                model.addAttribute("breeds", breedRepository.findAll());
                model.addAttribute("userSpecies", userSpeciesRepository.findByOwnerIdOrderByNameAsc(owner.getId()));
                return "pets/form";
            }

            pet.setOwner(owner);

            if (avatarFile != null && !avatarFile.isEmpty()) {
                String avatarUrl = saveAvatar(avatarFile);
                pet.setAvatarUrl(avatarUrl);
            }

            pet.setStatus(PetStatus.ACTIVE);
            petService.save(pet);

            redirectAttributes.addFlashAttribute("success", "Da them thu cung thanh cong!");
            return "redirect:/pets";

        } catch (Exception e) {
            User owner = userDetails.getUser();
            model.addAttribute("error", "Loi: " + e.getMessage());
            model.addAttribute("categories", petCategoryRepository.findAll());
            model.addAttribute("breeds", breedRepository.findAll());
            model.addAttribute("userSpecies", userSpeciesRepository.findByOwnerIdOrderByNameAsc(owner.getId()));
            return "pets/form";
        }
    }

    // ==================== Form sửa thú cưng ====================

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetailsImpl userDetails,
                              Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(id, userDetails);
            model.addAttribute("pet", pet);
            model.addAttribute("categories", petCategoryRepository.findAll());
            model.addAttribute("breeds", breedRepository.findAll());
            model.addAttribute("userSpecies", userSpeciesRepository.findByOwnerIdOrderByNameAsc(userDetails.getUser().getId()));
            return "pets/form";
        } catch (Exception e) {
            return "redirect:/pets";
        }
    }

    // ==================== Xử lý cập nhật thú cưng ====================

    @PostMapping("/{id}/update")
    @Transactional
    public String updatePet(@PathVariable Long id,
                           @Valid @ModelAttribute Pet pet,
                           BindingResult result,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) Long userSpeciesId,
                           @RequestParam(required = false) Long breedId,
                           @RequestParam(required = false) Long userBreedId,
                           @RequestParam(required = false) MultipartFile avatarFile,
                           @AuthenticationPrincipal UserDetailsImpl userDetails,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet existing = checkPetAccess(id, userDetails);

            pet.setId(id);
            pet.setOwner(existing.getOwner());
            pet.setCreatedAt(existing.getCreatedAt());

            User owner = userDetails.getUser();

            // Ưu tiên loài tự tạo, không thì dùng hệ thống
            if (userSpeciesId != null && userSpeciesId > 0) {
                UserSpecies us = userSpeciesRepository.findByIdAndOwnerId(userSpeciesId, owner.getId()).orElse(null);
                if (us == null) throw new IllegalArgumentException("Khong tim thay loai tu tao");
                pet.setUserSpecies(us);
                pet.setCategory(null);

                if (userBreedId != null && userBreedId > 0) {
                    pet.setUserBreed(userBreedRepository.findById(userBreedId).orElse(null));
                } else {
                    pet.setUserBreed(null);
                }
            } else if (categoryId != null && categoryId > 0) {
                PetCategory category = petCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Khong tim thay loai thu cung"));
                pet.setCategory(category);
                pet.setUserSpecies(null);

                if (breedId != null && breedId > 0) {
                    pet.setBreed(breedRepository.findById(breedId).orElse(null));
                } else {
                    pet.setBreed(null);
                }
                pet.setUserBreed(null);
            } else {
                // Giữ nguyên nếu không chọn gì
                pet.setUserSpecies(existing.getUserSpecies());
                pet.setUserBreed(existing.getUserBreed());
            }

            // Xử lý avatar mới
            if (avatarFile != null && !avatarFile.isEmpty()) {
                String avatarUrl = saveAvatar(avatarFile);
                pet.setAvatarUrl(avatarUrl);
            } else {
                // Giữ avatar cũ
                pet.setAvatarUrl(existing.getAvatarUrl());
            }

            petService.save(pet);
            redirectAttributes.addFlashAttribute("success", "Da cap nhat thu cung thanh cong!");
            return "redirect:/pets";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Loi: " + e.getMessage());
            return "redirect:/pets";
        }
    }

    // ==================== Xóa thú cưng ====================

    @PostMapping("/{id}/delete")
    public String deletePet(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetailsImpl userDetails,
                            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(id, userDetails);
            petService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Da xoa thu cung: " + pet.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Loi khi xoa: " + e.getMessage());
        }

        return "redirect:/pets";
    }

    // ==================== API lấy giống theo loại ====================

    @GetMapping("/api/breeds")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getBreedsByCategory(@RequestParam Long categoryId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Breed b : breedRepository.findByCategoryId(categoryId)) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", b.getId());
            row.put("name", b.getName());
            out.add(row);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(out);
    }

    /** Chỉ trả giống của loài tự tạo thuộc user đang đăng nhập; tránh cache + JSON entity (lazy species). */
    @GetMapping("/api/user-breeds")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getUserBreedsBySpecies(
            @RequestParam Long speciesId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(List.of());
        }
        UserSpecies species = userSpeciesRepository
                .findByIdAndOwnerId(speciesId, userDetails.getUser().getId())
                .orElse(null);
        if (species == null) {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(List.of());
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (UserBreed b : userBreedRepository.findBySpeciesIdOrderByNameAsc(speciesId)) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", b.getId());
            row.put("name", b.getName());
            out.add(row);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(out);
    }

    // ==================== Tổng quan sức khỏe ====================

    @GetMapping("/{petId}/health")
    public String petHealth(@PathVariable Long petId,
                           @AuthenticationPrincipal UserDetailsImpl userDetails,
                           Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(petId, userDetails);
            HealthService.PetHealthSummary summary = healthService.getPetHealthSummary(petId);
            model.addAttribute("pet", pet);
            model.addAttribute("summary", summary);
            return "pets/health/overview";
        } catch (Exception e) {
            return "redirect:/pets";
        }
    }

    // ==================== Hồ sơ bệnh án ====================

    @GetMapping("/{petId}/medical-records")
    public String medicalRecords(@PathVariable Long petId,
                                @AuthenticationPrincipal UserDetailsImpl userDetails,
                                Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(petId, userDetails);
            List<MedicalRecord> records = healthService.getMedicalRecordsByPetId(petId);
            model.addAttribute("pet", pet);
            model.addAttribute("records", records);
            return "pets/health/medical-records";
        } catch (Exception e) {
            return "redirect:/pets";
        }
    }

    @GetMapping("/{petId}/medical-records/new")
    public String medicalRecordForm(@PathVariable Long petId,
                                   @AuthenticationPrincipal UserDetailsImpl userDetails,
                                   Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(petId, userDetails);
            model.addAttribute("pet", pet);
            model.addAttribute("record", new MedicalRecord());
            model.addAttribute("veterinarians", userRepository.findByRoleName(com.db7.j2ee_quanlythucung.entity.Role.RoleType.ROLE_VET));
            return "pets/health/medical-record-form";
        } catch (Exception e) {
            return "redirect:/pets";
        }
    }

    @PostMapping("/{petId}/medical-records")
    public String saveMedicalRecord(@PathVariable Long petId,
                                    @Valid @ModelAttribute MedicalRecord record,
                                    BindingResult result,
                                    @AuthenticationPrincipal UserDetailsImpl userDetails,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(petId, userDetails);

            if (result.hasErrors()) {
                model.addAttribute("pet", pet);
                model.addAttribute("veterinarians", userRepository.findByRoleName(com.db7.j2ee_quanlythucung.entity.Role.RoleType.ROLE_VET));
                return "pets/health/medical-record-form";
            }

            record.setPet(pet);
            record.setRecordDate(java.time.LocalDateTime.now());
            healthService.saveMedicalRecord(record);
            redirectAttributes.addFlashAttribute("success", "Da luu ho so benh an!");
            return "redirect:/pets/" + petId + "/medical-records";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Loi: " + e.getMessage());
            return "redirect:/pets";
        }
    }

    @PostMapping("/{petId}/medical-records/{recordId}/delete")
    public String deleteMedicalRecord(@PathVariable Long petId,
                                      @PathVariable Long recordId,
                                      @AuthenticationPrincipal UserDetailsImpl userDetails,
                                      RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            checkPetAccess(petId, userDetails);
            healthService.deleteMedicalRecord(recordId);
            redirectAttributes.addFlashAttribute("success", "Da xoa ho so benh an!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Loi: " + e.getMessage());
        }

        return "redirect:/pets/" + petId + "/medical-records";
    }

    // ==================== Tiêm phòng ====================

    @GetMapping("/{petId}/vaccinations")
    public String vaccinations(@PathVariable Long petId,
                              @AuthenticationPrincipal UserDetailsImpl userDetails,
                              Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(petId, userDetails);
            List<Vaccination> vaccinations = healthService.getVaccinationsByPetId(petId);
            model.addAttribute("pet", pet);
            model.addAttribute("vaccinations", vaccinations);
            model.addAttribute("totalVaccinations", vaccinations.size());
            if (vaccinations != null) {
                String thisMonth = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                int thisMonthCount = (int) vaccinations.stream()
                    .filter(v -> v.getVaccinationDate() != null
                        && v.getVaccinationDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")).equals(thisMonth))
                    .count();
                int upcomingSoonCount = (int) vaccinations.stream()
                    .filter(v -> v.getNextDueDate() != null
                        && v.getNextDueDate().isBefore(java.time.LocalDate.now().plusDays(7)))
                    .count();
                model.addAttribute("thisMonthCount", thisMonthCount);
                model.addAttribute("upcomingSoonCount", upcomingSoonCount);
            }
            return "pets/health/vaccinations";
        } catch (Exception e) {
            return "redirect:/pets";
        }
    }

    @GetMapping("/{petId}/vaccinations/new")
    public String vaccinationForm(@PathVariable Long petId,
                                  @AuthenticationPrincipal UserDetailsImpl userDetails,
                                  Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(petId, userDetails);
            model.addAttribute("pet", pet);
            model.addAttribute("vaccination", new Vaccination());
            model.addAttribute("veterinarians", userRepository.findByRoleName(com.db7.j2ee_quanlythucung.entity.Role.RoleType.ROLE_VET));
            return "pets/health/vaccination-form";
        } catch (Exception e) {
            return "redirect:/pets";
        }
    }

    @PostMapping("/{petId}/vaccinations")
    public String saveVaccination(@PathVariable Long petId,
                                  @Valid @ModelAttribute Vaccination vaccination,
                                  BindingResult result,
                                  @AuthenticationPrincipal UserDetailsImpl userDetails,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(petId, userDetails);

            if (result.hasErrors()) {
                model.addAttribute("pet", pet);
                model.addAttribute("veterinarians", userRepository.findByRoleName(com.db7.j2ee_quanlythucung.entity.Role.RoleType.ROLE_VET));
                return "pets/health/vaccination-form";
            }

            vaccination.setPet(pet);
            healthService.saveVaccination(vaccination);
            redirectAttributes.addFlashAttribute("success", "Da luu lich tiem phong!");
            return "redirect:/pets/" + petId + "/vaccinations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Loi: " + e.getMessage());
            return "redirect:/pets";
        }
    }

    @PostMapping("/{petId}/vaccinations/{vaccinationId}/delete")
    public String deleteVaccination(@PathVariable Long petId,
                                    @PathVariable Long vaccinationId,
                                    @AuthenticationPrincipal UserDetailsImpl userDetails,
                                    RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            checkPetAccess(petId, userDetails);
            healthService.deleteVaccination(vaccinationId);
            redirectAttributes.addFlashAttribute("success", "Da xoa lich tiem phong!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Loi: " + e.getMessage());
        }

        return "redirect:/pets/" + petId + "/vaccinations";
    }

    // ==================== Theo dõi cân nặng ====================

    @GetMapping("/{petId}/weight")
    public String weightTracking(@PathVariable Long petId,
                                @AuthenticationPrincipal UserDetailsImpl userDetails,
                                Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(petId, userDetails);
            List<WeightRecord> records = healthService.getWeightRecordsByPetId(petId);
            List<Map<String, String>> chartData = healthService.getWeightRecordsFlat(petId);
            WeightRecord latest = healthService.getLatestWeightRecord(petId);
            model.addAttribute("pet", pet);
            model.addAttribute("records", records);
            model.addAttribute("chartData", chartData);
            model.addAttribute("latestWeight", latest);
            return "pets/health/weight";
        } catch (Exception e) {
            return "redirect:/pets";
        }
    }

    @GetMapping("/{petId}/weight/new")
    public String weightForm(@PathVariable Long petId,
                            @AuthenticationPrincipal UserDetailsImpl userDetails,
                            Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(petId, userDetails);
            model.addAttribute("pet", pet);
            model.addAttribute("weightRecord", new WeightRecord());
            return "pets/health/weight-form";
        } catch (Exception e) {
            return "redirect:/pets";
        }
    }

    @PostMapping("/{petId}/weight")
    public String saveWeight(@PathVariable Long petId,
                             @Valid @ModelAttribute WeightRecord weightRecord,
                             BindingResult result,
                             @AuthenticationPrincipal UserDetailsImpl userDetails,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Pet pet = checkPetAccess(petId, userDetails);

            if (result.hasErrors()) {
                model.addAttribute("pet", pet);
                return "pets/health/weight-form";
            }

            weightRecord.setPet(pet);
            weightRecord.setRecordedBy(userDetails.getUser());
            healthService.saveWeightRecord(weightRecord);

            pet.setWeight(weightRecord.getWeight());
            petService.save(pet);

            redirectAttributes.addFlashAttribute("success", "Đã cập nhật cân nặng!");
            return "redirect:/pets/" + petId + "/weight";
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = e.getClass().getSimpleName();
            }
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu: " + msg);
            return "redirect:/pets/" + petId + "/weight/new";
        }
    }

    @PostMapping("/{petId}/weight/{weightId}/delete")
    public String deleteWeight(@PathVariable Long petId,
                                @PathVariable Long weightId,
                                @AuthenticationPrincipal UserDetailsImpl userDetails,
                                RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            checkPetAccess(petId, userDetails);
            healthService.deleteWeightRecord(weightId);
            redirectAttributes.addFlashAttribute("success", "Da xoa ban ghi can nang!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Loi: " + e.getMessage());
        }

        return "redirect:/pets/" + petId + "/weight";
    }
}
