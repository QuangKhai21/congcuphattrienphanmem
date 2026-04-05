package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.Pet;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.entity.VetAnswer;
import com.db7.j2ee_quanlythucung.entity.VetQuestion;
import com.db7.j2ee_quanlythucung.entity.VetQuestion.QuestionCategory;
import com.db7.j2ee_quanlythucung.entity.VetQuestion.QuestionStatus;
import com.db7.j2ee_quanlythucung.repository.PetRepository;
import com.db7.j2ee_quanlythucung.repository.UserRepository;
import com.db7.j2ee_quanlythucung.repository.VetAnswerRepository;
import com.db7.j2ee_quanlythucung.repository.VetQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VetQAService {

    private final VetQuestionRepository questionRepository;
    private final VetAnswerRepository answerRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<VetQuestion> findById(Long id) {
        return questionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<VetQuestion> findAll(int page, int size) {
        return questionRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<VetQuestion> findByStatus(QuestionStatus status, int page, int size) {
        return questionRepository.findByStatus(status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<VetQuestion> findByCategory(QuestionCategory category, int page, int size) {
        return questionRepository.findByCategory(category, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<VetQuestion> findByAuthor(Long authorId, int page, int size) {
        return questionRepository.findByAuthorId(authorId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Page<VetQuestion> findPopular(int page, int size) {
        return questionRepository.findAnsweredQuestions(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<VetQuestion> search(String keyword) {
        return questionRepository.searchQuestions(keyword, PageRequest.of(0, 20)).getContent();
    }

    @Transactional
    public VetQuestion createQuestion(String title, String content, Long petId, Long authorId,
                                     QuestionCategory category) {
        Pet pet = petId != null ? petRepository.findById(petId).orElse(null) : null;
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        VetQuestion question = VetQuestion.builder()
                .title(title)
                .content(content)
                .pet(pet)
                .author(author)
                .category(category != null ? category : QuestionCategory.GENERAL)
                .status(QuestionStatus.PENDING)
                .viewCount(0)
                .likeCount(0)
                .isPublic(true)
                .build();

        return questionRepository.save(question);
    }

    @Transactional
    public VetAnswer answerQuestion(Long questionId, String content, Long authorId) {
        VetQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        VetAnswer answer = VetAnswer.builder()
                .question(question)
                .content(content)
                .author(author)
                .isAccepted(false)
                .likeCount(0)
                .viewCount(0)
                .build();

        VetAnswer savedAnswer = answerRepository.save(answer);

        question.setAnswer(savedAnswer);
        question.setStatus(QuestionStatus.ANSWERED);
        question.setAnsweredAt(LocalDateTime.now());
        question.setAnsweredBy(author);

        questionRepository.save(question);
        return savedAnswer;
    }

    @Transactional
    public VetQuestion updateQuestion(Long id, String title, String content, QuestionCategory category) {
        VetQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));

        if (title != null) question.setTitle(title);
        if (content != null) question.setContent(content);
        if (category != null) question.setCategory(category);

        return questionRepository.save(question);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        questionRepository.findById(id).ifPresent(q -> {
            q.setViewCount((q.getViewCount() != null ? q.getViewCount() : 0) + 1);
            questionRepository.save(q);
        });
    }

    @Transactional
    public void likeQuestion(Long id) {
        questionRepository.findById(id).ifPresent(q -> {
            q.setLikeCount((q.getLikeCount() != null ? q.getLikeCount() : 0) + 1);
            questionRepository.save(q);
        });
    }

    @Transactional
    public void closeQuestion(Long id) {
        questionRepository.findById(id).ifPresent(q -> {
            q.setStatus(QuestionStatus.CLOSED);
            questionRepository.save(q);
        });
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return questionRepository.countByStatus(QuestionStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countAnswered() {
        return questionRepository.countByStatus(QuestionStatus.ANSWERED);
    }

    @Transactional(readOnly = true)
    public Page<VetQuestion> findByVet(Long vetId, Pageable pageable) {
        return questionRepository.findByAnsweredBy(vetId, pageable);
    }
}
