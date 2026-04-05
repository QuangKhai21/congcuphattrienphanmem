package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.Breed;
import com.db7.j2ee_quanlythucung.entity.PetCategory;
import com.db7.j2ee_quanlythucung.repository.BreedRepository;
import com.db7.j2ee_quanlythucung.repository.PetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BreedService {

    private final BreedRepository breedRepository;
    private final PetCategoryRepository petCategoryRepository;

    @Transactional(readOnly = true)
    public List<Breed> findAll() {
        return breedRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Breed> findById(Long id) {
        return breedRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Breed> findByCategoryId(Long categoryId) {
        return breedRepository.findByCategoryId(categoryId);
    }

    @Transactional
    public Breed save(Breed breed, Long categoryId) {
        if (categoryId != null) {
            PetCategory category = petCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loài"));
            breed.setCategory(category);
        }
        return breedRepository.save(breed);
    }

    @Transactional
    public Breed save(Breed breed) {
        return breedRepository.save(breed);
    }

    @Transactional
    public void deleteById(Long id) {
        breedRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByNameAndCategoryId(String name, Long categoryId) {
        return breedRepository.existsByNameIgnoreCaseAndCategoryId(name, categoryId);
    }
}
