package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.*;
import com.db7.j2ee_quanlythucung.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.db7.j2ee_quanlythucung.entity.Role.RoleType;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AIHealthAssessmentService aiHealthAssessmentService;
    private final NekomimiAIService nekomimiAIService;

    @Transactional(readOnly = true)
    public Page<Conversation> findUserConversations(Long userId, int page, int size) {
        return conversationRepository.findByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt")));
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> findById(Long id) {
        return conversationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> findByUserAndVet(Long userId, Long vetId) {
        return conversationRepository.findByUserAndVet(userId, vetId);
    }

    /** Tìm hoặc tạo cuộc trò chuyện vet. */
    @Transactional
    public Conversation getOrCreateVetConversation(Long userId, Long vetId) {
        Optional<Conversation> existing = conversationRepository.findByUserAndVet(userId, vetId);
        if (existing.isPresent()) {
            return existing.get();
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        User vet = userRepository.findById(vetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ"));
        boolean isVetUser = Boolean.TRUE.equals(vet.getIsVet())
                || vet.getRoles().stream().anyMatch(r -> Role.RoleType.ROLE_VET.equals(r.getName()));
        if (!isVetUser) {
            throw new IllegalArgumentException("Người dùng không phải bác sĩ đã đăng ký");
        }

        Conversation conv = Conversation.builder()
                .type(Conversation.Type.VET)
                .vet(vet)
                .build();
        conv.addParticipant(user);
        conv.addParticipant(vet);
        return conversationRepository.save(conv);
    }

    /** Gửi tin nhắn cho vet (chỉ lưu tin nhắn, không auto-reply). */
    @Transactional
    public ConversationMessage sendVetMessage(Long conversationId, Long senderId, String content) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cuộc trò chuyện"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người gửi"));

        // Xác định senderType dựa trên vai trò của người gửi
        ConversationMessage.SenderType senderType = ConversationMessage.SenderType.USER;
        if (Boolean.TRUE.equals(sender.getIsVet()) || sender.getRoles().stream()
                .anyMatch(r -> RoleType.ROLE_VET.equals(r.getName()))) {
            senderType = ConversationMessage.SenderType.VET;
        }

        ConversationMessage msg = ConversationMessage.builder()
                .conversation(conv)
                .sender(sender)
                .content(content)
                .senderType(senderType)
                .isRead(false)
                .build();
        return messageRepository.save(msg);
    }

    /** Bác sĩ trả lời tin nhắn trong cuộc trò chuyện (chỉ lưu, không auto-reply). */
    @Transactional
    public ConversationMessage sendVetReply(Long conversationId, Long vetId, String content) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cuộc trò chuyện"));
        User vet = userRepository.findById(vetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ"));

        ConversationMessage msg = ConversationMessage.builder()
                .conversation(conv)
                .sender(vet)
                .content(content)
                .senderType(ConversationMessage.SenderType.VET)
                .isRead(false)
                .build();
        return messageRepository.save(msg);
    }

    /** Gửi tin nhắn AI (Nekomimi). */
    @Transactional
    public ConversationMessage sendAIMessage(Long conversationId, String userMessage) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cuộc trò chuyện"));

        // Lấy AI user (tìm user đầu tiên có role AI hoặc fallback)
        User aiUser = userRepository.findByUsername("nekomimi-ai")
                .or(() -> userRepository.findByRoleName(RoleType.ROLE_VET).stream().findFirst())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy AI user"));

        // Lấy userId từ participants (lấy user đầu tiên không phải AI)
        Long userId = conv.getParticipants().stream()
                .map(p -> p.getUser())
                .filter(u -> u != null && !u.getId().equals(aiUser.getId()))
                .findFirst()
                .map(User::getId)
                .orElse(null);

        // Lấy lịch sử hội thoại gần đây (last 10 messages)
        List<ConversationMessage> history = messageRepository
                .findByConversationIdOrderBySentAtAsc(conversationId, PageRequest.of(0, 10)).getContent();

        // Lưu tin nhắn user (hiển thị là user nhưng thực ra là AI đang đóng vai user trong context)
        ConversationMessage userMsg = ConversationMessage.builder()
                .conversation(conv)
                .sender(aiUser)
                .content(userMessage)
                .senderType(ConversationMessage.SenderType.USER) // Hiển thị như tin nhắn của user
                .isRead(true)
                .build();
        messageRepository.save(userMsg);

        // Gọi AI để lấy phản hồi (với context hội thoại)
        String aiResponse = nekomimiAIService.getResponse(userId, userMessage, history);

        ConversationMessage reply = ConversationMessage.builder()
                .conversation(conv)
                .sender(aiUser)
                .content(aiResponse)
                .senderType(ConversationMessage.SenderType.AI)
                .aiResponse(aiResponse)
                .isRead(false)
                .build();
        return messageRepository.save(reply);
    }

    /** Tìm hoặc tạo cuộc trò chuyện AI cho user. */
    @Transactional
    public Conversation getOrCreateAIConversation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        List<Conversation> aiConvs = conversationRepository.findAIByUserId(userId,
                PageRequest.of(0, 1)).getContent();
        Conversation conv;
        if (!aiConvs.isEmpty()) {
            conv = aiConvs.get(0);
        } else {
            conv = Conversation.builder()
                    .type(Conversation.Type.AI)
                    .build();
            conv.addParticipant(user);
            conv = conversationRepository.save(conv);
        }
        return conv;
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> getMessages(Long conversationId, int page, int size) {
        return messageRepository.findByConversationId(
                conversationId,
                PageRequest.of(page, size)).getContent();
    }

    @Transactional
    public void markRead(Long conversationId, Long userId) {
        messageRepository.markRead(conversationId, userId);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long conversationId, Long userId) {
        return messageRepository.countUnread(conversationId, userId);
    }

    @Transactional(readOnly = true)
    public long countConsultationsByVet(Long vetId) {
        return conversationRepository.countConsultationsByVet(vetId);
    }

    /**
     * Tìm hoặc tạo cuộc trò chuyện PRIVATE giữa 2 user.
     * Nếu đã tồn tại (dù user nào là người tạo trước) thì trả về cuộc trò chuyện đó.
     */
    @Transactional
    public Conversation getOrCreatePrivateConversation(Long userId1, Long userId2) {
        // Ưu tiên tìm theo chiều: userId1 request, targetUserId = userId2
        Optional<Conversation> existing = conversationRepository
                .findPrivateConversationByTarget(userId1, userId2);
        if (existing.isPresent()) {
            return existing.get();
        }
        // Thử tìm theo chiều ngược (userId2 request trước, targetUserId = userId1)
        existing = conversationRepository.findPrivateConversationByTarget(userId2, userId1);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Không tìm thấy → tạo mới
        User u1 = userRepository.findById(userId1)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng 1"));
        User u2 = userRepository.findById(userId2)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng 2"));
        if (u1.getId().equals(u2.getId())) {
            throw new IllegalArgumentException("Không thể chat với chính mình");
        }

        Conversation conv = Conversation.builder()
                .type(Conversation.Type.PRIVATE)
                .targetUserId(userId2) // người nhận (người còn lại)
                .build();
        conv.addParticipant(u1);
        conv.addParticipant(u2);
        return conversationRepository.save(conv);
    }

    /**
     * Gửi tin nhắn trong cuộc trò chuyện PRIVATE giữa 2 user.
     */
    @Transactional
    public ConversationMessage sendPrivateMessage(Long conversationId, Long senderId, String content) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cuộc trò chuyện"));
        if (conv.getType() != Conversation.Type.PRIVATE) {
            throw new IllegalArgumentException("Cuộc trò chuyện không phải loại PRIVATE");
        }
        // Kiểm tra người gửi có trong cuộc trò chuyện không
        boolean isParticipant = conv.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(senderId) && Boolean.TRUE.equals(p.getIsActive()));
        if (!isParticipant) {
            throw new IllegalArgumentException("Người dùng không tham gia cuộc trò chuyện này");
        }
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người gửi"));

        ConversationMessage msg = ConversationMessage.builder()
                .conversation(conv)
                .sender(sender)
                .content(content)
                .senderType(ConversationMessage.SenderType.USER)
                .isRead(false)
                .build();
        ConversationMessage saved = messageRepository.save(msg);
        conv.addMessage(saved); // cập nhật lastMessageAt
        conversationRepository.save(conv);
        return saved;
    }
}
