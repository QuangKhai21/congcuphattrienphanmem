package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.PetService;
import com.db7.j2ee_quanlythucung.entity.PetService.ServiceType;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.ServiceLocatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Controller
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceLocatorController {

    private final ServiceLocatorService serviceLocatorService;

    private boolean isAdmin(UserDetailsImpl userDetails) {
        if (userDetails == null) return false;
        return userDetails.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) ServiceType type,
                       Model model) {
        page = Math.max(0, page);
        Page<PetService> services;
        if (type != null) {
            services = serviceLocatorService.findByType(type, PageRequest.of(page, 12));
            model.addAttribute("selectedType", type);
        } else {
            services = serviceLocatorService.findAll(PageRequest.of(page, 12));
        }

        model.addAttribute("services", services.getContent());
        model.addAttribute("totalPages", services.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("serviceTypes", ServiceType.values());
        model.addAttribute("serviceCounts", getServiceCounts());
        return "services/list";
    }

    @GetMapping("/nearby")
    public String nearby(@RequestParam(required = false) Double lat,
                        @RequestParam(required = false) Double lng,
                        @RequestParam(required = false) ServiceType type,
                        @RequestParam(defaultValue = "10") Double radius,
                        Model model) {
        model.addAttribute("selectedType", type);
        model.addAttribute("radius", radius);
        model.addAttribute("serviceTypes", ServiceType.values());

        if (lat != null && lng != null) {
            List<PetService> services;
            if (type != null) {
                services = serviceLocatorService.findServicesByTypeInArea(type, lat, lng, radius);
            } else {
                services = serviceLocatorService.findServicesInArea(lat, lng, radius);
            }
            model.addAttribute("services", services);
            model.addAttribute("centerLat", lat);
            model.addAttribute("centerLng", lng);
            model.addAttribute("hasLocation", true);
        } else {
            model.addAttribute("hasLocation", false);
        }

        return "services/nearby";
    }

    @GetMapping("/24h")
    public String services24h(@RequestParam(required = false) Double lat,
                              @RequestParam(required = false) Double lng,
                              @RequestParam(defaultValue = "10") Double radius,
                              Model model) {
        double defLat = 10.8231;
        double defLng = 106.6297;
        List<PetService> services;
        if (lat != null && lng != null) {
            services = serviceLocatorService.findServices24h(lat, lng, radius);
        } else {
            services = serviceLocatorService.findServices24h(defLat, defLng, radius);
        }
        model.addAttribute("services", services);
        model.addAttribute("centerLat", lat != null ? lat : defLat);
        model.addAttribute("centerLng", lng != null ? lng : defLng);
        model.addAttribute("hasLocation", lat != null && lng != null);
        model.addAttribute("is24h", true);
        return "services/nearby";
    }

    @GetMapping("/pet-taxi")
    public String petTaxi(@RequestParam(required = false) Double lat,
                          @RequestParam(required = false) Double lng,
                          @RequestParam(defaultValue = "10") Double radius,
                          Model model) {
        double defLat = 10.8231;
        double defLng = 106.6297;
        List<PetService> services;
        if (lat != null && lng != null) {
            services = serviceLocatorService.findPetTaxis(lat, lng, radius);
        } else {
            services = serviceLocatorService.findServicesByTypeInArea(ServiceType.PET_TAXI, defLat, defLng, radius);
        }
        model.addAttribute("services", services);
        model.addAttribute("selectedType", ServiceType.PET_TAXI);
        model.addAttribute("centerLat", lat != null ? lat : defLat);
        model.addAttribute("centerLng", lng != null ? lng : defLng);
        model.addAttribute("hasLocation", lat != null && lng != null);
        return "services/nearby";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        PetService service = serviceLocatorService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
        model.addAttribute("service", service);
        return "services/detail";
    }

    @GetMapping("/search")
    public String search(@RequestParam String keyword,
                         @RequestParam(required = false) ServiceType type,
                         @RequestParam(required = false) Double lat,
                         @RequestParam(required = false) Double lng,
                         Model model) {
        List<PetService> results;
        if (lat != null && lng != null && type != null) {
            results = serviceLocatorService.findServicesByTypeInArea(type, lat, lng, 50.0);
            results = results.stream()
                    .filter(s -> s.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                                 (s.getAddress() != null && s.getAddress().toLowerCase().contains(keyword.toLowerCase())))
                    .toList();
        } else {
            results = serviceLocatorService.searchServices(keyword);
        }
        model.addAttribute("services", results);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedType", type);
        model.addAttribute("isSearch", true);
        model.addAttribute("serviceTypes", ServiceType.values());
        model.addAttribute("totalPages", 1);
        model.addAttribute("currentPage", 0);
        return "services/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("service", PetService.builder()
                .status(PetService.ServiceStatus.ACTIVE)
                .is24h(false)
                .hasPickup(false)
                .rating(0.0)
                .reviewCount(0)
                .build());
        return "services/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PetService service = serviceLocatorService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
        model.addAttribute("service", service);
        return "services/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute PetService service,
                       RedirectAttributes redirectAttributes) {
        try {
            if (service.getStatus() == null) {
                service.setStatus(PetService.ServiceStatus.ACTIVE);
            }
            serviceLocatorService.save(service);
            redirectAttributes.addFlashAttribute("success", "Đã lưu dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/services";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            serviceLocatorService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa dịch vụ!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/services";
    }

    @GetMapping("/api/nearby")
    @ResponseBody
    public List<PetService> nearbyApi(@RequestParam Double lat,
                                       @RequestParam Double lng,
                                       @RequestParam(required = false) ServiceType type,
                                       @RequestParam(defaultValue = "10") Double radius) {
        if (type != null) {
            return serviceLocatorService.findServicesByTypeInArea(type, lat, lng, radius);
        }
        return serviceLocatorService.findServicesInArea(lat, lng, radius);
    }

    private Map<ServiceType, Long> getServiceCounts() {
        Map<ServiceType, Long> counts = new LinkedHashMap<>();
        for (ServiceType type : ServiceType.values()) {
            counts.put(type, serviceLocatorService.countByType(type));
        }
        return counts;
    }
}
