package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.PetService;
import com.db7.j2ee_quanlythucung.entity.PetService.ServiceStatus;
import com.db7.j2ee_quanlythucung.entity.PetService.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetServiceRepository extends JpaRepository<PetService, Long> {

    Page<PetService> findByServiceType(ServiceType type, Pageable pageable);

    Page<PetService> findByServiceTypeAndStatus(ServiceType type, ServiceStatus status, Pageable pageable);

    List<PetService> findByServiceTypeAndStatus(ServiceType type, ServiceStatus status);

    @Query("SELECT s FROM PetService s WHERE s.status = 'ACTIVE' AND " +
           "(s.lat BETWEEN :minLat AND :maxLat) AND " +
           "(s.lng BETWEEN :minLng AND :maxLng)")
    List<PetService> findServicesInArea(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);

    @Query("SELECT s FROM PetService s WHERE s.status = 'ACTIVE' AND " +
           "(s.lat BETWEEN :minLat AND :maxLat) AND " +
           "(s.lng BETWEEN :minLng AND :maxLng) AND " +
           "s.serviceType = :type")
    List<PetService> findServicesByTypeInArea(
            @Param("type") ServiceType type,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);

    List<PetService> findByIs24hTrueAndStatus(ServiceStatus status);

    List<PetService> findByHasPickupTrueAndStatus(ServiceStatus status);

    @Query("SELECT s FROM PetService s WHERE s.status = 'ACTIVE' AND " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           "OR LOWER(s.address) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    List<PetService> searchServices(@Param("keyword") String keyword);

    long countByServiceType(ServiceType type);

    long countByStatus(ServiceStatus status);
}
