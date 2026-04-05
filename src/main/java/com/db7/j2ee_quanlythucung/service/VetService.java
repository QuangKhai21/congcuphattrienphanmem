package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.Role;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.repository.VetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VetService {

    private final VetRepository vetRepository;

    @Transactional(readOnly = true)
    public Page<User> findAll(int page, int size) {
        return vetRepository.findAllVets(Role.RoleType.ROLE_VET,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "rating")));
    }

    @Transactional(readOnly = true)
    public Page<User> search(String specialization, String province, String city, String keyword, int page, int size) {
        String spec = (specialization != null && !specialization.isBlank()) ? specialization.trim() : null;
        String prov = (province != null && !province.isBlank()) ? province.trim() : null;
        String c = (city != null && !city.isBlank()) ? city.trim() : null;
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return vetRepository.searchVets(Role.RoleType.ROLE_VET, spec, prov, c, kw,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "rating")));
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return vetRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<String> getSpecializations() {
        return vetRepository.findDistinctSpecializations(Role.RoleType.ROLE_VET);
    }

    @Transactional(readOnly = true)
    public List<String> getProvinces() {
        return vetRepository.findDistinctProvinces(Role.RoleType.ROLE_VET);
    }

    @Transactional(readOnly = true)
    public long count() {
        return vetRepository.findAllVets(Role.RoleType.ROLE_VET, PageRequest.of(0, 1)).getTotalElements();
    }
}
