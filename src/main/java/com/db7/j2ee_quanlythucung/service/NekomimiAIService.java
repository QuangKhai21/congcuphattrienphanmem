package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.ConversationMessage;
import com.db7.j2ee_quanlythucung.repository.ConversationMessageRepository;
import com.db7.j2ee_quanlythucung.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NekomimiAIService {

    private final ConversationMessageRepository messageRepository;
    private final PetRepository petRepository;

    // AI API Configuration
    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${ai.openrouter.endpoint:https://openrouter.ai/api/v1/chat/completions}")
    private String openRouterEndpoint;

    @Value("${ai.model:google/gemini-2.0-flash-001}")
    private String aiModel;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    // Per-user conversation context (in-memory, with fallback for multi-instance)
    private final Map<Long, UserConversationContext> userContexts = new ConcurrentHashMap<>();

    // System prompt cho AI
    private static final String SYSTEM_PROMPT = """
        Bạn là Nekomimi AI - trợ lý ảo của Pet Care, một ứng dụng quản lý thú cưng.
        
        Bạn là một cô gái mèo (nekomimi) dễ thương, thân thiện và nhiệt tình.
        
        KIM CHỈ NAM HÀNH VI:
        1. Luôn trả lời bằng tiếng Việt, thân thiện và dễ hiểu
        2. Sử dụng emoji phù hợp (😺🐾🍽️💉❤️‍🔥🩺)
        3. Không thay thế tư vấn bác sĩ thú y chuyên nghiệp
        4. Với triệu chứng nghiêm trọng, khuyên user đưa thú cưng đi khám ngay
        5. Cung cấp thông tin hữu ích về: dinh dưỡng, tiêm phòng, chăm sóc, bệnh thường gặp
        6. Có thể điều hướng đến các trang: /pets, /vet-qa, /appointments, /health/dashboard
        7. Luôn nhắc nhở Hotline: 1900 6750 cho trường hợp cấp cứu
        8. Nếu câu hỏi lặp lại, trả lời ngắn gọn và gợi ý chủ đề mới
        9. Tuyệt đối không bịa đặt thông tin y khoa
        10. Khi được hỏi về thuốc/liều lượng, khuyên user hỏi bác sĩ
        
        PHONG CÁCH TRẢ LỜI:
        - Nửa đầu: chào hỏi nhẹ nhàng, gọi "bạn" hoặc "bạn ơi"
        - Nửa sau: cung cấp thông tin cụ thể, có cấu trúc (danh sách, bước)
        - Kết thúc: gợi ý hành động tiếp theo hoặc hỏi thêm
        
        Ví dụ phong cách:
        "Chào bạn! 😺 Mình là Nekomimi, trợ lý Pet Care nè~
        Về [chủ đề], mình chia sẻ vài điều nhé:
        • [Điểm 1]
        • [Điểm 2]
        Bạn có thắc mắc gì thêm thì hỏi mình nha! 🐾"
        """;

    /**
     * Lấy response từ AI (ưu tiên OpenAI/Gemini, fallback rule-based)
     */
    @Transactional(readOnly = true)
    public String getResponse(Long userId, String userMessage, List<ConversationMessage> conversationHistory) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Bạn ơi, bạn nhắn gì cho mình với? 😺";
        }

        String cleanedMessage = userMessage.trim();
        
        // Kiểm tra lịch sử hội thoại để tránh lặp lại
        UserConversationContext context = userContexts.computeIfAbsent(
            userId, k -> new UserConversationContext());
        
        // Kiểm tra câu hỏi trùng lặp
        if (context.isDuplicateQuestion(cleanedMessage)) {
            return context.getLastResponse() + 
                   "\n\n💡 P/S: Mình đã trả lời câu này rồi nha! Bạn muốn hỏi thêm về vấn đề gì khác không? 🐾";
        }

        String response;

        // Ưu tiên dùng AI API nếu có
        if (aiEnabled && hasAiApiKey()) {
            try {
                response = callAiApi(userId, cleanedMessage, conversationHistory);
            } catch (Exception e) {
                log.warn("AI API call failed, falling back to rule-based: {}", e.getMessage());
                response = getRuleBasedResponse(cleanedMessage, context);
            }
        } else {
            response = getRuleBasedResponse(cleanedMessage, context);
        }

        // Lưu context để tránh lặp lại
        context.addExchange(cleanedMessage, response);

        return response;
    }

    /**
     * Kiểm tra xem có API key AI không
     */
    private boolean hasAiApiKey() {
        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            return false;
        }
        String k = openRouterApiKey.trim();
        if (k.contains("YOUR_OPENROUTER") || k.contains("xxxxxxxx")) {
            return false;
        }
        return k.startsWith("sk-or-");
    }

    /**
     * Gọi AI API (OpenRouter - hỗ trợ nhiều model)
     */
    private String callAiApi(Long userId, String userMessage, List<ConversationMessage> history) {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openRouterApiKey.trim());
        // OpenRouter khuyến nghị (một số trường hợp bắt buộc để xếp hạng / tránh chặn)
        headers.add("HTTP-Referer", appBaseUrl);
        headers.add("X-Title", "Pet Care Nekomimi");
        
        // Build conversation history for context
        String conversationContext = buildConversationContext(history);
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        // System message
        messages.add(Map.of(
            "role", "system",
            "content", SYSTEM_PROMPT
        ));
        
        // Add conversation history (last 10 messages for context window)
        if (conversationContext != null && !conversationContext.isBlank()) {
            messages.add(Map.of(
                "role", "system",
                "content", "Lịch sử trò chuyện gần đây:\n" + conversationContext
            ));
        }
        
        // Add user message with pet context
        String enrichedMessage = enrichWithPetContext(userId, userMessage);
        messages.add(Map.of(
            "role", "user", 
            "content", enrichedMessage
        ));
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiModel);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1000);
        requestBody.put("temperature", 0.8); // Slightly creative
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                openRouterEndpoint, request, Map.class);

            if (response != null && response.containsKey("error")) {
                log.error("OpenRouter trả về lỗi trong body: {}", response.get("error"));
                throw new RuntimeException("OpenRouter error: " + response.get("error"));
            }

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null) {
                        Object content = message.get("content");
                        if (content instanceof String s && !s.isBlank()) {
                            log.info("OpenRouter OK, model={}", aiModel);
                            return s.trim();
                        }
                    }
                }
            }
            log.warn("OpenRouter phản hồi không có choices/message.content: {}", response);
        } catch (HttpStatusCodeException e) {
            log.error("OpenRouter HTTP {} — body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenRouter HTTP " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Lỗi gọi OpenRouter: {}", e.getMessage(), e);
            throw new RuntimeException("AI API error", e);
        }

        return getRuleBasedResponse(userMessage, userContexts.get(userId));
    }

    /**
     * Bổ sung context về thú cưng của user
     */
    private String enrichWithPetContext(Long userId, String userMessage) {
        StringBuilder context = new StringBuilder();
        
        try {
            var pets = petRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
            if (!pets.isEmpty()) {
                context.append("📋 Thông tin thú cưng của bạn:\n");
                pets.stream().limit(3).forEach(pet -> {
                    String catName = pet.getCategory() != null ? pet.getCategory().getName() : "thú cưng";
                    String w = pet.getWeight() != null && !pet.getWeight().isBlank()
                            ? pet.getWeight().trim() + " kg"
                            : "chưa ghi cân";
                    context.append(String.format("- %s (%s, %s)\n", pet.getName(), catName, w));
                });
            }
        } catch (Exception e) {
            log.debug("Could not fetch pet context: {}", e.getMessage());
        }
        
        context.append("\nCâu hỏi của bạn: ").append(userMessage);
        return context.toString();
    }

    /**
     * Build conversation context string
     */
    private String buildConversationContext(List<ConversationMessage> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        
        return history.stream()
            .sorted(Comparator.comparing(ConversationMessage::getSentAt))
            .skip(Math.max(0, history.size() - 10))
            .map(m -> {
                String sender = m.getSenderType() == ConversationMessage.SenderType.AI 
                    ? "Nekomimi" : "Bạn";
                return sender + ": " + m.getContent();
            })
            .collect(Collectors.joining("\n"));
    }

    /**
     * Rule-based response system với NLP nâng cao
     */
    private String getRuleBasedResponse(String message, UserConversationContext context) {
        String lowerMsg = normalizeText(message);
        
        // Phân tích intent chính
        Intent intent = classifyIntent(lowerMsg, context);
        
        // Xử lý theo intent
        return switch (intent) {
            case GREETING -> generateGreeting(context);
            case HEALTH_SYMPTOM -> generateSymptomResponse(lowerMsg);
            case NUTRITION -> generateNutritionResponse(lowerMsg);
            case VACCINATION -> generateVaccinationResponse(lowerMsg);
            case FIND_VET -> generateFindVetResponse();
            case APPOINTMENT -> generateAppointmentResponse();
            case PET_INFO -> generatePetInfoResponse(context);
            case EMERGENCY -> generateEmergencyResponse();
            case BEHAVIOR -> generateBehaviorResponse(lowerMsg);
            case HYGIENE -> generateHygieneResponse();
            case LOST_PET -> generateLostPetResponse();
            case THANKS -> generateThanksResponse();
            case REPEAT -> "Bạn ơi, mình đã trả lời về chủ đề này rồi nha! 😺\n\n" +
                    "Bạn có muốn hỏi về vấn đề khác không? Mình có thể giúp:\n" +
                    "• 🍽️ Dinh dưỡng &amp; thức ăn\n" +
                    "• 💉 Tiêm phòng\n" +
                    "• ❤️‍🔥 Sức khỏe &amp; triệu chứng\n" +
                    "• 🔍 Tìm bác sĩ gần bạn\n" +
                    "• 📅 Đặt lịch hẹn";
            case GENERAL -> generateGeneralResponse(lowerMsg);
        };
    }

    private enum Intent {
        GREETING, HEALTH_SYMPTOM, NUTRITION, VACCINATION, 
        FIND_VET, APPOINTMENT, PET_INFO, EMERGENCY, 
        BEHAVIOR, HYGIENE, LOST_PET, THANKS, REPEAT, GENERAL
    }

    private Intent classifyIntent(String msg, UserConversationContext context) {
        // Emergency - ưu tiên cao nhất
        if (Pattern.compile("\\b(cấp cứu|khẩn|co giật|mất ý thức|khó thở nặng|ngạt|chảy máu|ngộ độc)\\b").matcher(msg).find()) {
            return Intent.EMERGENCY;
        }
        
        // Chào hỏi
        if (Pattern.compile("\\b(chào|xin chào|hello|hi|hey|chao|hola|alo|good morning|good afternoon)\\b").matcher(msg).find()) {
            return Intent.GREETING;
        }
        
        // Triệu chứng bệnh
        if (Pattern.compile("\\b(ho|nôn|ói|tiêu chảy|sốt|biếng ăn|chán ăn|Ỉa|đi ngoài|lười|bỏ ăn|ngứa|rụng lông|sung mắt|mỏi mắt|sưng|đau|khó thở|thở nhanh)\\b").matcher(msg).find()) {
            return Intent.HEALTH_SYMPTOM;
        }
        
        // Dinh dưỡng
        if (Pattern.compile("\\b(ăn|uống|thức ăn|dinh dưỡng|cho ăn|thịt|cá|rau|vitamin|bổ sung|sữa|chế độ ăn|tăng cân|giảm cân)\\b").matcher(msg).find()) {
            return Intent.NUTRITION;
        }
        
        // Tiêm phòng
        if (Pattern.compile("\\b(tiêm|tiêm phòng|vaccine|vacxin|sốt dại|phòng bệnh|lịch tiêm|mũi tiêm)\\b").matcher(msg).find()) {
            return Intent.VACCINATION;
        }
        
        // Tìm bác sĩ
        if (Pattern.compile("\\b(bác sĩ|thú y|tìm bác sĩ|danh sách bác sĩ|phòng khám|tư vấn)\\b").matcher(msg).find()) {
            return Intent.FIND_VET;
        }
        
        // Lịch hẹn
        if (Pattern.compile("\\b(lịch hẹn|đặt lịch|hẹn khám|booking|đặt lịch)\\b").matcher(msg).find()) {
            return Intent.APPOINTMENT;
        }
        
        // Thông tin thú cưng
        if (Pattern.compile("\\b(thú cưng|cần nặng|tuổi|giống|loài|bệnh|chăm sóc)\\b").matcher(msg).find()) {
            return Intent.PET_INFO;
        }
        
        // Hành vi
        if (Pattern.compile("\\b(hành vi|cư xử|nghịch|cắn|gầu|sợ|lo lắng|stress|hung dữ)\\b").matcher(msg).find()) {
            return Intent.BEHAVIOR;
        }
        
        // Vệ sinh
        if (Pattern.compile("\\b(tắm|vệ sinh|cắt móng|chải lông|sạch sẽ|hygiene)\\b").matcher(msg).find()) {
            return Intent.HYGIENE;
        }
        
        // Thú lạc
        if (Pattern.compile("\\b(lạc|mất|thất lạc|tìm thú|tìm chó|tìm mèo)\\b").matcher(msg).find()) {
            return Intent.LOST_PET;
        }
        
        // Cảm ơn
        if (Pattern.compile("\\b(cảm ơn|thank|thanks|tạm biệt|bye|ok|okay)\\b").matcher(msg).find()) {
            return Intent.THANKS;
        }
        
        // Hỏi lại chủ đề đã hỏi
        if (context != null && context.hasRecentTopic(msg)) {
            return Intent.REPEAT;
        }
        
        return Intent.GENERAL;
    }

    private String normalizeText(String text) {
        return text.toLowerCase()
            .replaceAll("\\s+", " ")
            .replaceAll("[^a-zàáảãạăằắặẳẵâầấậẩẫèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹđ\\s]", " ")
            .trim();
    }

    private String generateGreeting(UserConversationContext context) {
        String[] greetings = {
            "Chào bạn! 😺✨ Mình là Nekomimi, trợ lý Pet Care nè~",
            "Meo meo~ Chào bạn! 😺 Mình là Nekomimi, rất vui được gặp bạn!",
            "Hi bạn! 😸 Mình là Nekomimi AI, trợ lý chăm sóc thú cưng của Pet Care!",
            "Xin chào! 😺🐾 Mình là Nekomimi, có gì mình giúp bạn được không?"
        };
        
        String greeting = greetings[(int) (Math.random() * greetings.length)];
        String help = """
            Mình có thể giúp bạn về:
            🐾 <strong>Chăm sóc thú cưng</strong>: dinh dưỡng, vệ sinh, tập luyện
            ❤️‍🔥 <strong>Theo dõi sức khỏe</strong>: cân nặng, tiêm phòng, triệu chứng bệnh
            🔍 <strong>Tìm dịch vụ</strong>: bác sĩ thú y, lịch hẹn, tìm thú lạc
            💡 Gì cũng hỏi mình nha!
            """;
        
        // Thêm context nếu có thú cưng
        if (context != null && context.hasPets()) {
            help = "Nhìn thấy bạn có thú cưng rồi nè! " + help;
        }
        
        return greeting + "\n\n" + help;
    }

    private String generateSymptomResponse(String msg) {
        // Phân tích triệu chứng cụ thể
        StringBuilder response = new StringBuilder();
        response.append("😿 Về triệu chứng bạn mô tả, mình chia sẻ vài điều nhé:\n\n");
        
        if (msg.contains("ho")) {
            response.append("""
                <strong>🌡️ Nếu bé ho:</strong>
                • Theo dõi 2-3 ngày, xem có kèm sốt không
                • Cho uống nước ấm, giữ ấm cổ họng
                • Nếu ho > 1 tuần hoặc ho ra máu → đi khám ngay
                """);
        }
        
        if (msg.contains("nôn") || msg.contains("ói")) {
            response.append("""
                <strong>🤮 Nếu bé nôn/ói:</strong>
                • Nhịn ăn 12-24h (với chó), 24-48h (với mèo)
                • Cho uống nước nhỏ giọt, tránh nước lớn
                • Nếu nôn > 3 lần hoặc có máu → cấp cứu ngay
                """);
        }
        
        if (msg.contains("tiêu chảy") || msg.contains("đi ngoài")) {
            response.append("""
                <strong>💩 Nếu bé bị tiêu chảy:</strong>
                • Cho ăn thức ăn nhạt (cơm trắng, thịt gà nạc)
                • Bổ sung nước điện giải
                • Nếu kéo dài > 2 ngày hoặc có máu → đi khám
                """);
        }
        
        if (msg.contains("sốt")) {
            response.append("""
                <strong>🌡️ Nếu bé sốt:</strong>
                • Đo nhiệt độ (bình thường: chó 38-39.2°C, mèo 38-39.5°C)
                • Lau ấm bằng khăn ẩm vùng bẹn, tai
                • Nếu sốt > 40°C hoặc kèm các triệu chứng khác → đi khám
                """);
        }
        
        if (msg.contains("biếng ăn") || msg.contains("chán ăn") || msg.contains("bỏ ăn")) {
            response.append("""
                <strong>🍽️ Nếu bé biếng ăn:</strong>
                • Thử thay đổi loại thức ăn
                • Hâm nóng thức ăn để có mùi hấp dẫn hơn
                • Nếu bỏ ăn > 24h (chó) hoặc > 48h (mèo) → đi khám
                """);
        }
        
        response.append("""

            📞 <strong>Cấp cứu:</strong> Gọi <strong>1900 6750</strong> nếu:
            • Khó thở nặng, ho ra máu
            • Nôn liên tục không ngừng
            • Mất ý thức hoặc co giật
            • Bụng căng cứng, đau dữ dội

            💡 Bạn có thể <a href="/vet-qa">hỏi bác sĩ thú y</a> để được tư vấn cụ thể hơn nhé!
            """);
        
        return response.toString();
    }

    private String generateNutritionResponse(String msg) {
        StringBuilder response = new StringBuilder();
        response.append("🍽️ Về dinh dưỡng cho thú cưng, mình chia sẻ nhé:\n\n");
        
        response.append("""
            <strong>🐕 Chó:</strong>
            • 2 bữa/ngày (sáng + chiều tối)
            • Lượng thức ăn: ~2-3% trọng lượng cơ thể
            • Nên có thức ăn khô chuyên dụng kèm thức ăn tươi
            """);
        
        response.append("""
            
            <strong>🐱 Mèo:</strong>
            • 2-3 bữa nhỏ/ngày
            • Mèo có thể ăn tự do (dry food)
            • Cần đảm bảo nước uống đầy đủ
            """);
        
        if (msg.contains("thịt") || msg.contains("cá")) {
            response.append("""

                <strong>🥩 Thức ăn tươi cho bé:</strong>
                • Thịt gà, bò, cá nấu chín (không xương)
                • Không cho gia vị, hành, tỏi
                • Rau củ hấp (cà rốt, bí đỏ, bông cải)
                """);
        }
        
        response.append("""

            <strong>⚠️ Tuyệt đối tránh:</strong>
            • Chocolate (gây ngộ độc)
            • Hành, tỏi (gây thiếu máu)
            • Nho, nho khô (gây suy thận)
            • Xương gà, xương nhỏ (dễ hóc)
            • Bia, rượu, caffeine

            💡 Theo dõi cân nặng bé thường xuyên <a href="/pets">tại đây</a> nhé!
            """);
        
        return response.toString();
    }

    private String generateVaccinationResponse(String msg) {
        return """
            💉 Về tiêm phòng, đây là lịch trình cơ bản:

            <strong>🐕 Chó:</strong>
            • 6-8 tuần: Vaccine 5 bệnh (Care, Parvo, Viêm gan...)
            • 9-11 tuần: Mũi nhắc lại 1
            • 12-14 tuần: Mũi nhắc lại 2 + Vaccine dại
            • Hàng năm: Nhắc lại

            <strong>🐱 Mèo:</strong>
            • 8-9 tuần: Vaccine 3 bệnh (dịch mèo, viêm đường hô hấp)
            • 12 tuần: Nhắc lại + Vaccine dại
            • Hàng năm: Nhắc lại

            <strong>📋 Lưu ý:</strong>
            • Tiêm khi bé khỏe mạnh, không sốt
            • Sau tiêm có thể mệt 1-2 ngày là bình thường
            • Nếu sốt cao hoặc sưng nhiều → đi khám

            💾 Kiểm tra lịch sử tiêm của bé <a href="/pets">tại đây</a> nhé!
            """;
    }

    private String generateFindVetResponse() {
        return """
            🔍 Tìm bác sĩ thú y:

            Bạn có thể:
            • Xem danh sách bác sĩ và chat riêng <a href="/vet-qa">tại đây</a>
            • Đặt lịch hẹn khám <a href="/appointments">tại đây</a>
            • Gọi Hotline: <strong>1900 6750</strong>

            <strong>💡 Mẹo chọn bác sĩ:</strong>
            • Ưu tiên bác sĩ có chuyên ngành phù hợp với vấn đề của bé
            • Đọc đánh giá từ người dùng khác
            • Chọn phòng khám gần bạn để tiện theo dõi

            <strong>⚠️ Lưu ý:</strong> Với các vấn đề nghiêm trọng, hãy đưa bé đến phòng khám gần nhất thay vì chỉ tư vấn online nhé!
            """;
    }

    private String generateAppointmentResponse() {
        return """
            📅 Đặt lịch hẹn khám:

            Bạn có thể đặt lịch hẹn <a href="/appointments">tại đây</a>

            <strong>📋 Chuẩn bị trước khi khám:</strong>
            • Mang theo sổ tiêm phòng (nếu có)
            • Liệt kê các triệu chứng bất thường
            • Nhịn ăn 8-12 tiếng nếu cần xét nghiệm
            • Chuẩn bị câu hỏi muốn hỏi bác sĩ

            <strong>💡 Hoặc:</strong>
            • Hỏi bác sĩ online trước <a href="/vet-qa">tại đây</a>
            • Gọi Hotline: <strong>1900 6750</strong>
            """;
    }

    private String generatePetInfoResponse(UserConversationContext context) {
        return """
            🐾 Về thông tin thú cưng:

            Mỗi loài và giống có đặc điểm khác nhau:

            <strong>🐕 Chó:</strong>
            • Tuổi thọ trung bình: 10-15 năm
            • Cần đi dạo và vận động hàng ngày
            • Tiêm phòng đầy đủ theo lịch

            <strong>🐱 Mèo:</strong>
            • Tuổi thọ trung bình: 12-18 năm
            • Cần không gian leo trèo và scratch post
            • Chú ý vệ sinh hàng ngày

            💡 Xem thông tin chi tiết và theo dõi sức khỏe <a href="/pets">tại đây</a> nhé!
            """;
    }

    private String generateEmergencyResponse() {
        return """
            🚨 <strong>CẢNH BÁO CẤP CỨU!</strong>

            Bạn cần liên hệ ngay:
            📞 <strong>Hotline: 1900 6750</strong>

            <strong>⚠️ Đưa bé đến phòng khám gần nhất NGAY nếu:</strong>
            • Khó thở, thở nhanh/ khò khè
            • Co giật, mất ý thức
            • Chảy máu không cầm được
            • Nôn ói liên tục, có máu
            • Bụng căng cứng, đau dữ dội
            • Ngộ độc (đã ăn chocolate, hành, thuốc...)

            <strong>🚑 Trong lúc chờ:</strong>
            • Giữ bình tĩnh
            • Không tự ý cho uống thuốc
            • Giữ bé ấm, hạn chế di chuyển
            • Gọi điện báo trước cho phòng khám

            💡 Sau khi xử lý khẩn, hãy đưa bé đi khám để kiểm tra kỹ hơn nhé!
            """;
    }

    private String generateBehaviorResponse(String msg) {
        StringBuilder response = new StringBuilder();
        response.append("🧠 Về hành vi thú cưng, mình chia sẻ nhé:\n\n");
        
        if (msg.contains("cắn")) {
            response.append("""
                <strong>🦷 Nếu bé có thói cắn:</strong>
                • Có thể do sợ hãi, đau, hoặc bảo vệ lãnh thổ
                • Không phạt bé khi cắn
                • Tham khảo huấn luyện viên chuyên nghiệp
                • Nếu cắn dữ dội → cần bác sĩ đánh giá
                """);
        }
        
        if (msg.contains("gầu") || msg.contains("đánh dấu")) {
            response.append("""
                <strong>🧩 Nếu bé gầu/đánh dấu lãnh thổ:</strong>
                • Cần triệt sản để giảm hành vi
                • Dọn sạch vết gầu bằng chất tẩy men
                • Tham khảo bác sĩ về thời điểm triệt sản phù hợp
                """);
        }
        
        if (msg.contains("sợ") || msg.contains("lo lắng") || msg.contains("stress")) {
            response.append("""
                <strong>😰 Nếu bé sợ/stress:</strong>
                • Tìm nguyên nhân gây stress
                • Tạo không gian yên tĩnh, an toàn
                • Sử dụng pheromone tổng hợp (Feliway/DAP)
                • Tham khảo bác sĩ về thuốc hỗ trợ nếu cần
                """);
        }
        
        response.append("""

            💡 Mỗi hành vi đều có nguyên nhân. Quan trọng là kiên nhẫn và yêu thương bé nha!

            🐾 <a href="/vet-qa">Hỏi bác sĩ</a> nếu hành vi ảnh hưởng nghiêm trọng đến cuộc sống của bé.
            """);
        
        return response.toString();
    }

    private String generateHygieneResponse() {
        return """
            🛁 Về vệ sinh thú cưng:

            <strong>🛁 Tắm rửa:</strong>
            • Chó: 1-2 tuần/lần (tùy giống)
            • Mèo: Thường tự làm sạch, có thể tắm 1-2 tháng/lần
            • Dùng sữa tắm chuyên dụng cho thú cưng

            <strong>🧹 Chải lông:</strong>
            • Chải hàng ngày với giống lông dài
            • Chải 2-3 lần/tuần với giống lông ngắn
            • Giúp phân phối dầu tự nhiên, giảm rụng lông

            <strong>✂️ Cắt móng:</strong>
            • Cắt 2-4 tuần/lần
            • Cẩn thận không cắt vào phần mạch máu (móng hồng)
            • Nếu móng đen, cắt từ từ từng lớp

            <strong>🦷 Vệ sinh răng miệng:</strong>
            • Đánh răng cho bé 2-3 lần/tuần
            • Dùng bàn chải và kem đánh răng chuyên dụng
            • Khám răng định kỳ 1 lần/năm
            """;
    }

    private String generateLostPetResponse() {
        return """
            🔎 Tìm thú cưng lạc:

            <strong>Bạn có thể:</strong>
            • Đăng tin tìm kiếm <a href="/lost-pets">tại đây</a>
            • Xem các báo cáo thú lạc gần đây
            • Chia sẻ thông tin để được nhiều người giúp đỡ

            <strong>🐾 Mẹo tìm nhanh:</strong>
            • Gọi tên bé thật nhiều
            • Để đồ có mùi quen thuộc của bé ở nơi bé hay đi
            • Thông báo cho chính quyền địa phương và cơ sở cứu hộ
            • Đăng ảnh rõ ràng, thông tin liên lạc rõ ràng

            <strong>⏰ Lưu ý:</strong>
            • Tìm sớm trong 24-48 giờ đầu là quan trọng nhất
            • Thú cưng có xu hướng ẩn náu gần nhà
            • Kiểm tra các nơi tối, ấm như gầm giường, tủ quần áo

            💡 Nếu bạn nhìn thấy thú lạc, hãy <a href="/lost-pets">đăng tin</a> để giúp reunite với chủ nhân nhé!
            """;
    }

    private String generateThanksResponse() {
        String[] responses = {
            "Cảm ơn bạn đã trò chuyện! 😺✨ Chúc bạn và thú cưng luôn khỏe mạnh nha! 🐾",
            "Yêu bạn lắm! 😸💕 Mình ở đây nếu bạn cần hỏi thêm gì nhé!",
            "Bye bye~ 😺🐾 Nhớ chăm sóc thật tốt bé yêu của bạn nha!",
            "Mình đi nghỉ đây~ 😺👋 Nếu cần gì thì quay lại hỏi mình nhé!"
        };
        
        String base = responses[(int) (Math.random() * responses.length)];
        
        return base + """

            💡 <strong>Liên hệ khi cần:</strong>
            • <a href="/vet-qa">Hỏi bác sĩ thú y</a>
            • <a href="/pets">Quản lý thú cưng</a>
            • Hotline: <strong>1900 6750</strong>

            🐾 Chúc bạn và bé yêu một ngày tốt lành! 💕
            """;
    }

    private String generateGeneralResponse(String msg) {
        StringBuilder response = new StringBuilder();
        response.append("😺 Cảm ơn bạn đã nhắn! Mình là Nekomimi, trợ lý Pet Care nè~\n\n");
        response.append("Mình có thể giúp bạn về nhiều chủ đề:\n\n");
        
        String[] topics = {
            "• 🍽️ Dinh dưỡng &amp; thức ăn cho thú cưng",
            "• 💉 Tiêm phòng &amp; lịch trình vaccine",
            "• ❤️‍🔥 Sức khỏe &amp; triệu chứng bệnh thường gặp",
            "• 🧠 Hành vi &amp; huấn luyện",
            "• 🛁 Vệ sinh &amp; chăm sóc hàng ngày",
            "• 🔍 Tìm bác sĩ thú y &amp; đặt lịch hẹn",
            "• 📋 Thông tin thú cưng &amp; giống loài",
            "• 🔎 Tìm thú cưng lạc"
        };
        
        for (String topic : topics) {
            response.append(topic).append("\n");
        }
        
        response.append("""

            💡 Bạn cứ hỏi thoải mái nha, mình sẽ cố gắng giúp!

            📞 Cần khẩn cấp? Gọi <strong>1900 6750</strong> hoặc <a href="/vet-qa">hỏi bác sĩ trực tuyến</a> nhé!
            """);
        
        return response.toString();
    }

    /**
     * User conversation context - theo dõi lịch sử hội thoại
     */
    private static class UserConversationContext {
        private final List<String> recentQuestions = new ArrayList<>();
        private final List<String> recentResponses = new ArrayList<>();
        private String lastTopic = null;
        private LocalDateTime lastInteraction;
        private boolean hasPetContext = false;

        public boolean isDuplicateQuestion(String question) {
            String normalized = normalizeForComparison(question);
            
            // Kiểm tra câu hỏi giống hệt
            for (String q : recentQuestions) {
                if (normalizeForComparison(q).equals(normalized)) {
                    return true;
                }
            }
            
            // Kiểm tra câu hỏi tương tự (same intent)
            for (int i = recentQuestions.size() - 1; i >= Math.max(0, recentQuestions.size() - 3); i--) {
                String recent = recentQuestions.get(i);
                if (similarIntent(normalized, normalizeForComparison(recent))) {
                    return true;
                }
            }
            
            return false;
        }

        public boolean hasRecentTopic(String question) {
            String normalized = normalizeForComparison(question);
            for (String q : recentQuestions) {
                if (similarIntent(normalized, normalizeForComparison(q))) {
                    return true;
                }
            }
            return false;
        }

        public String getLastResponse() {
            if (recentResponses.isEmpty()) {
                return "";
            }
            return recentResponses.get(recentResponses.size() - 1);
        }

        public boolean hasPets() {
            return hasPetContext;
        }

        public void addExchange(String question, String response) {
            // Giới hạn 10 câu hỏi gần nhất
            if (recentQuestions.size() >= 10) {
                recentQuestions.remove(0);
                recentResponses.remove(0);
            }
            
            recentQuestions.add(question);
            recentResponses.add(response);
            lastInteraction = LocalDateTime.now();
            
            // Extract topic
            lastTopic = extractTopic(question);
        }

        private String normalizeForComparison(String text) {
            return text.toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-zàáảãạăằắặẳẵâầấậẩẫèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹđ]", "")
                .trim();
        }

        private boolean similarIntent(String q1, String q2) {
            // Simple similarity check using common words
            Set<String> words1 = new HashSet<>(Arrays.asList(q1.split("\\s+")));
            Set<String> words2 = new HashSet<>(Arrays.asList(q2.split("\\s+")));
            
            // Remove common words
            words1.removeAll(Set.of("về", "cho", "và", "của", "có", "được", "mình", "bạn", "thú", "cưng", "pet"));
            words2.removeAll(Set.of("về", "cho", "và", "của", "có", "được", "mình", "bạn", "thú", "cưng", "pet"));
            
            // Calculate Jaccard similarity
            if (words1.isEmpty() || words2.isEmpty()) return false;
            
            Set<String> intersection = new HashSet<>(words1);
            intersection.retainAll(words2);
            
            Set<String> union = new HashSet<>(words1);
            union.addAll(words2);
            
            return (double) intersection.size() / union.size() > 0.5;
        }

        private String extractTopic(String question) {
            String lower = question.toLowerCase();
            
            if (lower.contains("tiêm") || lower.contains("vaccine")) return "vaccination";
            if (lower.contains("ăn") || lower.contains("dinh dưỡng")) return "nutrition";
            if (lower.contains("bệnh") || lower.contains("sức khỏe") || lower.contains("ho") || lower.contains("sốt")) return "health";
            if (lower.contains("bác sĩ")) return "vet";
            if (lower.contains("hành vi") || lower.contains("cắn")) return "behavior";
            if (lower.contains("tắm") || lower.contains("vệ sinh")) return "hygiene";
            
            return "general";
        }
    }
}
