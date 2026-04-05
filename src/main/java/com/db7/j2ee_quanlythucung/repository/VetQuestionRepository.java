package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.VetQuestion;
import com.db7.j2ee_quanlythucung.entity.VetQuestion.QuestionCategory;
import com.db7.j2ee_quanlythucung.entity.VetQuestion.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VetQuestionRepository extends JpaRepository<VetQuestion, Long> {

    Page<VetQuestion> findByStatus(QuestionStatus status, Pageable pageable);

    Page<VetQuestion> findByCategory(QuestionCategory category, Pageable pageable);

    Page<VetQuestion> findByAuthorId(Long authorId, Pageable pageable);

    Page<VetQuestion> findByPetOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT q FROM VetQuestion q WHERE q.answeredBy.id = :vetId ORDER BY q.answeredAt DESC")
    Page<VetQuestion> findByAnsweredBy(@Param("vetId") Long vetId, Pageable pageable);

    @Query("SELECT q FROM VetQuestion q WHERE q.status = 'ANSWERED' ORDER BY q.answeredAt DESC")
    Page<VetQuestion> findAnsweredQuestions(Pageable pageable);

    @Query("SELECT q FROM VetQuestion q WHERE q.isPublic = true AND q.status = 'ANSWERED' ORDER BY q.viewCount DESC")
    List<VetQuestion> findPopularQuestions(Pageable pageable);

    @Query("SELECT q FROM VetQuestion q WHERE " +
           "(LOWER(q.title) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           "OR LOWER(q.content) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    Page<VetQuestion> searchQuestions(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(QuestionStatus status);

    long countByCategory(QuestionCategory category);
}
