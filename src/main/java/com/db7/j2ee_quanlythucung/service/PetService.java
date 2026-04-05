package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.Pet;
import com.db7.j2ee_quanlythucung.entity.Pet.PetStatus;
import com.db7.j2ee_quanlythucung.repository.PetRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;

    @Transactional(readOnly = true)
    public Optional<Pet> findById(Long id) {
        return petRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Pet> findAll(Pageable pageable) {
        return petRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Pet> findByOwnerId(Long ownerId, Pageable pageable) {
        return petRepository.findByOwnerId(ownerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Pet> findByOwnerIdAndStatus(Long ownerId, PetStatus status, Pageable pageable) {
        return petRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Pet> search(String keyword, Long categoryId, Pageable pageable) {
        Specification<Pet> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"));
            }

            if (categoryId != null && categoryId > 0) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return petRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Pet> searchByOwner(String keyword, Long categoryId, Long ownerId, Pageable pageable) {
        Specification<Pet> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("owner").get("id"), ownerId));

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"));
            }

            if (categoryId != null && categoryId > 0) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return petRepository.findAll(spec, pageable);
    }

    @Transactional
    public Pet save(Pet pet) {
        return petRepository.save(pet);
    }

    @Transactional
    public void deleteById(Long id) {
        petRepository.deleteById(id);
    }
}
