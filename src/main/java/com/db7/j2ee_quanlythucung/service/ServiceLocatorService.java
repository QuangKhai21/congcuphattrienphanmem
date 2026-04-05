package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.PetService;
import com.db7.j2ee_quanlythucung.entity.PetService.ServiceStatus;
import com.db7.j2ee_quanlythucung.entity.PetService.ServiceType;
import com.db7.j2ee_quanlythucung.repository.PetServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceLocatorService {

    private final PetServiceRepository petServiceRepository;

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Transactional(readOnly = true)
    public Optional<PetService> findById(Long id) {
        return petServiceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<PetService> findAll(Pageable pageable) {
        return petServiceRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<PetService> findByType(ServiceType type, Pageable pageable) {
        return petServiceRepository.findByServiceTypeAndStatus(type, ServiceStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public List<PetService> findServicesInArea(Double centerLat, Double centerLng, Double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));
        List<PetService> services = petServiceRepository.findServicesInArea(
                centerLat - latDelta, centerLat + latDelta,
                centerLng - lngDelta, centerLng + lngDelta
        );
        return sortByDistance(services, centerLat, centerLng);
    }

    @Transactional(readOnly = true)
    public List<PetService> findServicesByTypeInArea(
            ServiceType type, Double centerLat, Double centerLng, Double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));
        List<PetService> services = petServiceRepository.findServicesByTypeInArea(
                type,
                centerLat - latDelta, centerLat + latDelta,
                centerLng - lngDelta, centerLng + lngDelta
        );
        return sortByDistance(services, centerLat, centerLng);
    }

    @Transactional(readOnly = true)
    public List<PetService> findServices24h(Double centerLat, Double centerLng, Double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));
        List<PetService> allServices = petServiceRepository.findServicesInArea(
                centerLat - latDelta, centerLat + latDelta,
                centerLng - lngDelta, centerLng + lngDelta
        );
        return allServices.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIs24h()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PetService> findPetTaxis(Double centerLat, Double centerLng, Double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));
        List<PetService> allServices = petServiceRepository.findServicesInArea(
                centerLat - latDelta, centerLat + latDelta,
                centerLng - lngDelta, centerLng + lngDelta
        );
        return allServices.stream()
                .filter(s -> s.getServiceType() == ServiceType.PET_TAXI)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PetService> searchServices(String keyword) {
        return petServiceRepository.searchServices(keyword);
    }

    @Transactional
    public PetService save(PetService service) {
        return petServiceRepository.save(service);
    }

    @Transactional
    public void deleteById(Long id) {
        petServiceRepository.deleteById(id);
    }

    private List<PetService> sortByDistance(List<PetService> services, Double centerLat, Double centerLng) {
        return services.stream()
                .sorted(Comparator.comparingDouble(s -> calculateDistance(
                        centerLat, centerLng, s.getLat(), s.getLng())))
                .collect(Collectors.toList());
    }

    public double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return Double.MAX_VALUE;
        }
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public long countByType(ServiceType type) {
        return petServiceRepository.countByServiceType(type);
    }
}
