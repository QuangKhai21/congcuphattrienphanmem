package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.*;
import com.db7.j2ee_quanlythucung.entity.Conversation.Type;
import com.db7.j2ee_quanlythucung.entity.Role.RoleType;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.*;
import com.db7.j2ee_quanlythucung.util.AvatarUrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class VetProfileController {

    private final VetService vetService;
    private final ConversationService conversationService;
    private final VetQAService vetQAService;
    private final AIHealthAssessmentService aiHealthAssessmentService;
    private final UserService userService;

    // ── Trang profile bác sĩ ──────────────────────────────────────────────
    @GetMapping("/vet/{id}")
    public String profile(@PathVariable Long id, Model model,
                          @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Optional<User> vetOpt = vetService.findById(id);
        if (vetOpt.isEmpty()) {
            return "redirect:/vet-qa";
        }
        User vet = vetOpt.get();
        boolean isPublicVet = Boolean.TRUE.equals(vet.getIsVet())
                || vet.getRoles().stream().anyMatch(r -> RoleType.ROLE_VET.equals(r.getName()));
        if (!isPublicVet) {
            return "redirect:/vet-qa";
        }

        long consultationCount = conversationService.countConsultationsByVet(id);
        Page<VetQuestion> recentQuestions = vetQAService.findByVet(id, PageRequest.of(0, 5));

        boolean hasConversation = false;
        Long conversationId = null;
        boolean isOwnProfile = false;
        if (userDetails != null) {
            Long meId = userDetails.getUser().getId();
            isOwnProfile = meId.equals(vet.getId());
            Optional<Conversation> conv = conversationService.findByUserAndVet(meId, id);
            hasConversation = conv.isPresent();
            conversationId = conv.map(Conversation::getId).orElse(null);
        }

        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("vetProfileAvatarUrl", AvatarUrlUtils.normalize(vet.getAvatarUrl()));
        model.addAttribute("vet", vet);
        model.addAttribute("consultationCount", consultationCount);
        model.addAttribute("recentQuestions", recentQuestions.getContent());
        model.addAttribute("hasConversation", hasConversation);
        model.addAttribute("conversationId", conversationId);
        return "vet-qa/profile";
    }

    // ── API: Tạo / lấy cuộc trò chuyện với bác sĩ ──────────────────────────
    @GetMapping("/api/conversations/vet/{vetId}")
    @ResponseBody
    public ResponseEntity<?> getOrCreateVetConversation(
            @PathVariable Long vetId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        Conversation conv = conversationService.getOrCreateVetConversation(
                userDetails.getUser().getId(), vetId);
        User vetUser = vetService.findById(vetId).orElse(null);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("conversationId", conv.getId());
        if (vetUser != null) {
            body.put("vetName", vetUser.getFullName());
            String spec = vetUser.getSpecialization();
            body.put("vetSpec", spec != null && !spec.isBlank() ? spec : "Bác sĩ thú y");
            body.put("vetAvatar", AvatarUrlUtils.normalize(vetUser.getAvatarUrl()));
        }
        return ResponseEntity.ok(body);
    }

    // ── API: Tạo / lấy cuộc trò chuyện PRIVATE giữa 2 user ────────────────
    @GetMapping("/api/conversations/private/{userId}")
    @ResponseBody
    public ResponseEntity<?> getOrCreatePrivateConversation(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        Long currentUserId = userDetails.getUser().getId();
        if (currentUserId.equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể chat với chính mình"));
        }
        Conversation conv = conversationService.getOrCreatePrivateConversation(currentUserId, userId);
        // Trả về thông tin người kia để frontend hiển thị
        Long otherId = conv.getTargetUserId();
        String otherName = "";
        String otherAvatar = null;
        // Lấy từ participants
        for (ConversationParticipant p : conv.getParticipants()) {
            if (!p.getUser().getId().equals(currentUserId)) {
                otherName = p.getUser().getFullName();
                otherAvatar = p.getUser().getAvatarUrl();
                break;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conv.getId());
        result.put("otherUserId", otherId);
        result.put("otherUserName", otherName);
        result.put("otherUserAvatar", AvatarUrlUtils.normalize(otherAvatar));
        return ResponseEntity.ok(result);
    }

    // ── API: Tìm kiếm user để bắt đầu chat ──────────────────────────────
    @GetMapping("/api/users/search")
    @ResponseBody
    public ResponseEntity<?> searchUsers(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        Page<User> results = userService.search(q, PageRequest.of(0, 20));
        Long currentId = userDetails.getUser().getId();
        List<Map<String, Object>> list = results.getContent().stream()
                .filter(u -> !u.getId().equals(currentId)) // loại trừ chính mình
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("fullName", u.getFullName());
                    m.put("avatarUrl", AvatarUrlUtils.normalize(u.getAvatarUrl()));
                    m.put("bio", u.getBio());
                    m.put("isVet", u.getIsVet());
                    return m;
                }).toList();
        return ResponseEntity.ok(list);
    }

    // ── API: Gửi tin nhắn cho bác sĩ ──────────────────────────────────────
    @PostMapping("/api/conversations/{id}/messages")
    @ResponseBody
    public ResponseEntity<?> sendMessage(
            @PathVariable Long id,
            @RequestParam String content,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tin nhắn trống"));
        }

        // Kiểm tra conversation
        Optional<Conversation> convOpt = conversationService.findById(id);
        if (convOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy cuộc trò chuyện"));
        }
        Conversation conv = convOpt.get();
        User sender = userDetails.getUser();

        // Kiểm tra nếu là cuộc trò chuyện với bác sĩ
        if (conv.getType() == Conversation.Type.VET) {
            // Nếu người gửi là bác sĩ được assign, cho phép gửi
            if (Boolean.TRUE.equals(sender.getIsVet()) || sender.getRoles().stream()
                    .anyMatch(r -> RoleType.ROLE_VET.equals(r.getName()))) {
                // Bác sĩ trả lời - tạo tin nhắn với senderType là VET
                ConversationMessage msg = conversationService.sendVetReply(id, sender.getId(), content.trim());
                return ResponseEntity.ok(messageToJson(msg, "VET"));
            } else {
                // User gửi tin nhắn cho bác sĩ
                ConversationMessage msg = conversationService.sendVetMessage(id, sender.getId(), content.trim());
                return ResponseEntity.ok(messageToJson(msg, "USER"));
            }
        } else if (conv.getType() == Conversation.Type.PRIVATE) {
            // Chat riêng giữa 2 user
            ConversationMessage msg = conversationService.sendPrivateMessage(id, sender.getId(), content.trim());
            return ResponseEntity.ok(messageToJson(msg, "USER"));
        } else {
            // Cuộc trò chuyện AI - user không thể gửi tin nhắn thủ công (đã có /api/ai/chat)
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể gửi tin nhắn trong cuộc trò chuyện này"));
        }
    }

    // ── API: Đánh dấu đã đọc tin nhắn ───────────────────────────────
    @PostMapping("/api/conversations/{id}/read")
    @ResponseBody
    public ResponseEntity<?> markRead(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        conversationService.markRead(id, userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ── API: Lấy tin nhắn cuộc trò chuyện ────────────────────────────────
    @GetMapping("/api/conversations/{id}/messages")
    @ResponseBody
    public ResponseEntity<?> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        conversationService.markRead(id, userDetails.getUser().getId());
        List<ConversationMessage> messages = conversationService.getMessages(id, page, 50);
        List<Map<String, Object>> result = messages.stream().map(m -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("content", m.getContent());
            item.put("senderType", m.getSenderType().name());
            item.put("senderId", m.getSender().getId());
            item.put("senderName", m.getSender().getFullName());
            item.put("senderAvatar", AvatarUrlUtils.normalize(m.getSender().getAvatarUrl()));
            item.put("sentAt", m.getSentAt().toString());
            item.put("isRead", m.getIsRead());
            return item;
        }).toList();
        return ResponseEntity.ok(result);
    }

    // ── API: Lấy danh sách cuộc trò chuyện (gửi cho chat panel) ──────────
    @GetMapping("/api/conversations")
    @ResponseBody
    public ResponseEntity<?> listConversations(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        Page<Conversation> convs = conversationService.findUserConversations(
                userDetails.getUser().getId(), 0, 50);
        Long currentUserId = userDetails.getUser().getId();
        List<Map<String, Object>> result = convs.getContent().stream().map(c -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("type", c.getType().name());
            item.put("vetId", c.getVet() != null ? c.getVet().getId() : null);
            item.put("vetName", c.getVet() != null ? c.getVet().getFullName() : "Nekomimi AI");
            item.put("vetSpec", c.getVet() != null ? c.getVet().getSpecialization() : null);
            item.put("vetAvatar", c.getVet() != null
                    ? AvatarUrlUtils.normalize(c.getVet().getAvatarUrl()) : null);
            item.put("createdAt", c.getCreatedAt().toString());
            item.put("lastMessageAt", c.getLastMessageAt() != null ? c.getLastMessageAt().toString() : null);
            long unread = conversationService.countUnread(c.getId(), currentUserId);
            item.put("unreadCount", unread);
            // PRIVATE / VET: luôn trả về người đối thoại (không phải chính mình)
            if (c.getType() == Type.PRIVATE || c.getType() == Type.VET) {
                for (ConversationParticipant p : c.getParticipants()) {
                    if (!p.getUser().getId().equals(currentUserId)) {
                        User peer = p.getUser();
                        item.put("otherUserId", peer.getId());
                        item.put("otherUserName", peer.getFullName());
                        item.put("otherUserAvatar", AvatarUrlUtils.normalize(peer.getAvatarUrl()));
                        if (c.getType() == Type.VET) {
                            boolean peerIsAssignedVet = c.getVet() != null
                                    && peer.getId().equals(c.getVet().getId());
                            String spec = peer.getSpecialization();
                            item.put("otherUserSpec", peerIsAssignedVet
                                    ? (spec != null && !spec.isBlank() ? spec : "Bác sĩ thú y")
                                    : "Khách hàng");
                        }
                        break;
                    }
                }
            }
            return item;
        }).toList();
        return ResponseEntity.ok(result);
    }

    // ── API: Gửi tin nhắn cho AI Nekomimi ───────────────────────────────
    @PostMapping("/api/ai/chat")
    @ResponseBody
    public ResponseEntity<?> chatWithAI(
            @RequestParam String message,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tin nhắn trống"));
        }

        // Tìm hoặc tạo conversation AI cho user
        Conversation conv = conversationService.getOrCreateAIConversation(userDetails.getUser().getId());

        // Lưu tin nhắn user và nhận câu trả lời AI (cùng lúc lưu vào DB)
        ConversationMessage replyMsg = conversationService.sendAIMessage(conv.getId(), message);

        // Trả về phản hồi AI kèm avatar
        User aiUser = replyMsg.getSender();
        String avatarUrl = aiUser != null
                ? AvatarUrlUtils.normalize(aiUser.getAvatarUrl()) : null;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", replyMsg.getId());
        response.put("content", replyMsg.getContent());
        response.put("senderType", "AI");
        response.put("senderName", "Nekomimi AI");
        response.put("senderAvatar", avatarUrl);
        response.put("sentAt", replyMsg.getSentAt().toString());
        response.put("conversationId", conv.getId());

        return ResponseEntity.ok(response);
    }

    // ── API: Lấy lịch sử chat AI ──────────────────────────────────────
    @GetMapping("/api/ai/messages")
    @ResponseBody
    public ResponseEntity<?> getAIMessages(
            @RequestParam(required = false) Long conversationId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        List<Map<String, Object>> result = new ArrayList<>();

        if (conversationId != null) {
            // Lấy tin nhắn từ conversation cụ thể
            List<ConversationMessage> msgs = conversationService.getMessages(conversationId, 0, 100);
            result = msgs.stream().map(m -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", m.getId());
                item.put("content", m.getContent());
                item.put("senderType", m.getSenderType().name());
                item.put("senderName", m.getSender() != null ? m.getSender().getFullName() : "Nekomimi AI");
                item.put("senderAvatar", m.getSender() != null
                        ? AvatarUrlUtils.normalize(m.getSender().getAvatarUrl()) : null);
                item.put("sentAt", m.getSentAt().toString());
                item.put("isRead", m.getIsRead());
                item.put("conversationId", m.getConversation().getId());
                return item;
            }).toList();
        } else {
            // Tìm AI conversation gần nhất: dùng phương thức repository có sẵn
            Conversation aiConv = null;
            try {
                for (Conversation c : conversationService.findUserConversations(
                        userDetails.getUser().getId(), 0, 10).getContent()) {
                    if (c.getType() == Type.AI) {
                        aiConv = c;
                        break;
                    }
                }
            } catch (Exception ignored) { /* không tìm thấy AI conversation */ }

            final Conversation foundConv = aiConv;
            if (foundConv != null) {
                List<ConversationMessage> msgs = conversationService.getMessages(foundConv.getId(), 0, 100);
                result = msgs.stream().map(m -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", m.getId());
                    item.put("content", m.getContent());
                    item.put("senderType", m.getSenderType().name());
                    item.put("senderName", m.getSender() != null ? m.getSender().getFullName() : "Nekomimi AI");
                    item.put("senderAvatar", m.getSender() != null
                            ? AvatarUrlUtils.normalize(m.getSender().getAvatarUrl()) : null);
                    item.put("sentAt", m.getSentAt().toString());
                    item.put("isRead", m.getIsRead());
                    item.put("conversationId", foundConv.getId());
                    return item;
                }).toList();
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * JSON cho tin nhắn — không dùng Map.of vì senderAvatar có thể null (Map.of cấm null → NPE → 500).
     */
    private static Map<String, Object> messageToJson(ConversationMessage msg, String senderType) {
        User u = msg.getSender();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", msg.getId());
        map.put("content", msg.getContent());
        map.put("sentAt", msg.getSentAt().toString());
        map.put("senderId", u.getId());
        map.put("senderName", u.getFullName());
        map.put("senderAvatar", AvatarUrlUtils.normalize(u.getAvatarUrl()));
        map.put("senderType", senderType);
        return map;
    }
}
