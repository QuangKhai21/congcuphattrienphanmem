package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.Appointment;
import com.db7.j2ee_quanlythucung.entity.Appointment.AppointmentStatus;
import com.db7.j2ee_quanlythucung.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public Optional<Appointment> findById(Long id) {
        return appointmentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Appointment> findAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Appointment> findByOwnerId(Long ownerId, Pageable pageable) {
        return appointmentRepository.findByPetOwnerId(ownerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable) {
        return appointmentRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByDateRange(start, end);
    }

    @Transactional
    public Appointment save(Appointment appointment) {
        boolean isNew = appointment.getId() == null;
        Appointment saved = appointmentRepository.save(appointment);
        if (isNew) {
            emailService.sendAppointmentConfirmation(saved);
        }
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        appointmentRepository.deleteById(id);
    }

    @Transactional
    public Appointment updateStatus(Long id, AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
        AppointmentStatus oldStatus = appointment.getStatus();
        appointment.setStatus(newStatus);
        Appointment saved = appointmentRepository.save(appointment);
        
        if (newStatus == AppointmentStatus.CANCELLED && oldStatus != AppointmentStatus.CANCELLED) {
            emailService.sendAppointmentCancellation(saved);
        }
        
        return saved;
    }
}
