package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    long countByPetId(Long petId);

    List<Appointment> findByPetOwnerId(Long ownerId);
    Page<Appointment> findByPetOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate BETWEEN :start AND :end ORDER BY a.appointmentDate")
    List<Appointment> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Page<Appointment> findByStatus(Appointment.AppointmentStatus status, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Appointment a")
    long countAllAppointments();

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :status")
    long countByStatus(@Param("status") Appointment.AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.pet.owner.id = :ownerId")
    long countByPetOwnerId(@Param("ownerId") Long ownerId);

    // Optimized: fetch pet+owner in single query
    @Query(value = "SELECT a FROM Appointment a JOIN FETCH a.pet p JOIN FETCH p.owner WHERE a.appointmentDate BETWEEN :start AND :end ORDER BY a.appointmentDate")
    List<Appointment> findByDateRangeWithPetAndOwner(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Optimized: fetch pet+owner for user's appointments in date range
    @Query(value = "SELECT a FROM Appointment a JOIN FETCH a.pet p JOIN FETCH p.owner WHERE a.appointmentDate BETWEEN :start AND :end AND p.owner.id = :ownerId ORDER BY a.appointmentDate")
    List<Appointment> findByDateRangeAndOwnerWithPetAndOwner(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("ownerId") Long ownerId);
}
