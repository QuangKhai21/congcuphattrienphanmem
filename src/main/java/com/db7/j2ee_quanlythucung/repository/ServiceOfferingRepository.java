package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    List<ServiceOffering> findByActiveTrue();
}
