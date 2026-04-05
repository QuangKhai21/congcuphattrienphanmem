package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.Role;
import com.db7.j2ee_quanlythucung.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VetRepository extends JpaRepository<User, Long> {

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE u.enabled = true AND " +
           "COALESCE(u.vetRegistrationBlocked, false) = false AND " +
           "(u.isVet = true OR r.name = :vetRole)")
    Page<User> findAllVets(@Param("vetRole") Role.RoleType vetRole, Pageable pageable);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE u.enabled = true AND " +
           "COALESCE(u.vetRegistrationBlocked, false) = false AND " +
           "(u.isVet = true OR r.name = :vetRole) AND " +
           "(:specialization IS NULL OR u.specialization = :specialization) AND " +
           "(:province IS NULL OR u.province = :province) AND " +
           "(:city IS NULL OR u.city = :city) AND " +
           "(:keyword IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "    OR LOWER(u.clinicName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "    OR LOWER(u.specialization) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "    OR LOWER(u.province) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchVets(@Param("vetRole") Role.RoleType vetRole,
                          @Param("specialization") String specialization,
                          @Param("province") String province,
                          @Param("city") String city,
                          @Param("keyword") String keyword,
                          Pageable pageable);

    @Query("SELECT DISTINCT u.specialization FROM User u LEFT JOIN u.roles r WHERE " +
           "COALESCE(u.vetRegistrationBlocked, false) = false AND " +
           "(u.isVet = true OR r.name = :vetRole) AND u.specialization IS NOT NULL AND u.specialization != ''")
    java.util.List<String> findDistinctSpecializations(@Param("vetRole") Role.RoleType vetRole);

    @Query("SELECT DISTINCT u.province FROM User u LEFT JOIN u.roles r WHERE " +
           "COALESCE(u.vetRegistrationBlocked, false) = false AND " +
           "(u.isVet = true OR r.name = :vetRole) AND u.province IS NOT NULL AND u.province != ''")
    java.util.List<String> findDistinctProvinces(@Param("vetRole") Role.RoleType vetRole);
}
