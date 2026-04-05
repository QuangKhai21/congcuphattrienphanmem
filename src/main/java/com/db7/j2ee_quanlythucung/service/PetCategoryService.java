package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.PetCategory;
import com.db7.j2ee_quanlythucung.repository.PetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PetCategoryService {

    private final PetCategoryRepository petCategoryRepository;

    @Transactional(readOnly = true)
    public List<PetCategory> findAll() {
        return petCategoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<PetCategory> findById(Long id) {
        return petCategoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<PetCategory> findByCode(String code) {
        return petCategoryRepository.findByCode(code);
    }

    @Transactional
    public PetCategory save(PetCategory category) {
        // Auto-generate code nếu trống
        if (category.getCode() == null || category.getCode().isBlank()) {
            category.setCode(generateCode(category.getName()));
        }
        return petCategoryRepository.save(category);
    }

    @Transactional
    public void deleteById(Long id) {
        petCategoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return petCategoryRepository.existsByCode(code);
    }

    private String generateCode(String name) {
        if (name == null || name.isBlank()) {
            return "NEW-" + System.currentTimeMillis();
        }
        // Chuyển thành không dấu và viết hoa
        String code = name.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]/g", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]/g", "e")
                .replaceAll("[ìíịỉĩ]/g", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]/g", "o")
                .replaceAll("[ùúụủũưừứựửữ]/g", "u")
                .replaceAll("[ỳýỵỷỹ]/g", "y")
                .replaceAll("[đ]/g", "d")
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return code.toUpperCase();
    }
}
