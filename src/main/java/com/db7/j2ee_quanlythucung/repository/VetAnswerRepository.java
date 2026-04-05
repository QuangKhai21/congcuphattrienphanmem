package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.VetAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VetAnswerRepository extends JpaRepository<VetAnswer, Long> {
    List<VetAnswer> findByQuestionIdOrderByCreatedAtDesc(Long questionId);
    void deleteByQuestionId(Long questionId);
}
