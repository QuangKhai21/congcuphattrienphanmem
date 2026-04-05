package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {

    List<Vaccination> findByPetIdOrderByVaccinationDateDesc(Long petId);

    @Query("SELECT v FROM Vaccination v WHERE v.pet.owner.id = :ownerId ORDER BY v.vaccinationDate DESC")
    List<Vaccination> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT v FROM Vaccination v WHERE v.nextDueDate <= :date AND (v.reminderSent = false OR v.reminderSent IS NULL)")
    List<Vaccination> findUpcomingReminders(@Param("date") LocalDate date);

    @Query("SELECT v FROM Vaccination v JOIN FETCH v.pet p JOIN FETCH p.owner WHERE v.nextDueDate <= :date AND (v.reminderSent = false OR v.reminderSent IS NULL) AND p.owner.id = :ownerId")
    List<Vaccination> findUpcomingRemindersByOwner(@Param("date") LocalDate date, @Param("ownerId") Long ownerId);

    @Query("SELECT v FROM Vaccination v WHERE v.nextDueDate BETWEEN :startDate AND :endDate")
    List<Vaccination> findByNextDueDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Optimized: fetch pet+owner in single query
    @Query(value = "SELECT v FROM Vaccination v JOIN FETCH v.pet p JOIN FETCH p.owner WHERE v.nextDueDate BETWEEN :startDate AND :endDate ORDER BY v.nextDueDate")
    List<Vaccination> findByNextDueDateBetweenWithPetAndOwner(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT v FROM Vaccination v JOIN FETCH v.pet p JOIN FETCH p.owner WHERE v.nextDueDate BETWEEN :startDate AND :endDate AND p.owner.id = :ownerId ORDER BY v.nextDueDate")
    List<Vaccination> findByNextDueDateBetweenAndOwnerWithPetAndOwner(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("ownerId") Long ownerId);

    @Query("SELECT v FROM Vaccination v WHERE v.pet.id = :petId AND v.vaccineName = :vaccineName ORDER BY v.vaccinationDate DESC")
    List<Vaccination> findByPetIdAndVaccineName(@Param("petId") Long petId, @Param("vaccineName") String vaccineName);

    long countByPetId(Long petId);
}
