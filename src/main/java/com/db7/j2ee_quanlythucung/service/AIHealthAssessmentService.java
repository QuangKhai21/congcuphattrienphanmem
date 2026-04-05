package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.HealthMetric;
import com.db7.j2ee_quanlythucung.entity.HealthMetric.MetricType;
import com.db7.j2ee_quanlythucung.entity.Pet;
import com.db7.j2ee_quanlythucung.entity.WeightRecord;
import com.db7.j2ee_quanlythucung.repository.HealthMetricRepository;
import com.db7.j2ee_quanlythucung.repository.WeightRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIHealthAssessmentService {

    private final HealthMetricRepository healthMetricRepository;
    private final WeightRecordRepository weightRecordRepository;

    private static final Map<MetricType, double[]> NORMAL_RANGES = new EnumMap<>(MetricType.class);
    private static final Map<String, Double> PET_CATEGORY_WEIGHTS = new HashMap<>();

    static {
        // [min_normal, max_normal, critical_low, critical_high]
        NORMAL_RANGES.put(MetricType.WEIGHT, new double[]{2.0, 60.0, 0.5, 80.0});
        NORMAL_RANGES.put(MetricType.TEMPERATURE, new double[]{38.0, 39.2, 37.0, 40.5});
        NORMAL_RANGES.put(MetricType.HEART_RATE, new double[]{60.0, 140.0, 40.0, 200.0});
        NORMAL_RANGES.put(MetricType.RESPIRATION, new double[]{10.0, 30.0, 5.0, 50.0});
        NORMAL_RANGES.put(MetricType.APPETITE, new double[]{5.0, 10.0, 1.0, 10.0});
        NORMAL_RANGES.put(MetricType.ACTIVITY, new double[]{5.0, 10.0, 1.0, 10.0});
        NORMAL_RANGES.put(MetricType.SLEEP_QUALITY, new double[]{5.0, 10.0, 1.0, 10.0});
        NORMAL_RANGES.put(MetricType.WATER_INTAKE, new double[]{200.0, 1000.0, 50.0, 2000.0});
        NORMAL_RANGES.put(MetricType.BOWEL_MOVEMENT, new double[]{3.0, 5.0, 1.0, 5.0});
        NORMAL_RANGES.put(MetricType.SEIZURE_COUNT, new double[]{0.0, 0.0, 0.0, 3.0});
        NORMAL_RANGES.put(MetricType.SEIZURE_DURATION, new double[]{0.0, 5.0, 0.0, 30.0});
        NORMAL_RANGES.put(MetricType.SEIZURE_SEVERITY, new double[]{0.0, 3.0, 0.0, 10.0});
        NORMAL_RANGES.put(MetricType.MEDICATION_DOSE, new double[]{0.0, 1000.0, 0.0, 5000.0});
        NORMAL_RANGES.put(MetricType.BLOOD_PRESSURE, new double[]{80.0, 140.0, 60.0, 180.0});
        NORMAL_RANGES.put(MetricType.BLOOD_SUGAR, new double[]{70.0, 120.0, 50.0, 200.0});
        NORMAL_RANGES.put(MetricType.HEIGHT, new double[]{10.0, 100.0, 5.0, 120.0});
    }

    @Transactional
    public void analyzeAndUpdateMetric(HealthMetric metric) {
        StringBuilder analysis = new StringBuilder();
        boolean alert = false;
        String alertMsg = null;

        double[] range = NORMAL_RANGES.get(metric.getMetricType());
        if (range != null) {
            double value = metric.getValue();
            double minNormal = range[0];
            double maxNormal = range[1];
            double critLow = range[2];
            double critHigh = range[3];

            double score = calculateMetricScore(value, minNormal, maxNormal, critLow, critHigh);
            String status = getStatusLabel(score);
            String advice = getAdvice(metric.getMetricType(), value, minNormal, maxNormal);

            analysis.append(String.format("📊 Chỉ số %s: **%.1f** (%s)\n",
                    getMetricLabel(metric.getMetricType()), value, status));
            analysis.append(advice);

            if (value < critLow || value > critHigh) {
                alert = true;
                alertMsg = String.format("⚠️ CẢNH BÁO: %s = %.1f vượt ngưỡng nguy hiểm!",
                        getMetricLabel(metric.getMetricType()), value);
                analysis.append("\n🚨 ").append(alertMsg);
            } else if (value < minNormal || value > maxNormal) {
                analysis.append("\n⚡ Cần theo dõi thêm.");
            }
        }

        metric.setAiAnalysis(analysis.toString());
        metric.setAlertTriggered(alert);
        metric.setAlertMessage(alertMsg);
        healthMetricRepository.save(metric);
    }

    public HealthAssessmentResult assessPetHealth(Pet pet) {
        List<HealthMetric> recentMetrics = healthMetricRepository
                .findByPetIdOrderByRecordedAtDesc(pet.getId());

        if (recentMetrics.isEmpty()) {
            return HealthAssessmentResult.builder()
                    .petId(pet.getId())
                    .overallScore(0.0)
                    .status("NO_DATA")
                    .message("Chưa có dữ liệu sức khỏe. Hãy bắt đầu ghi nhận các chỉ số.")
                    .metricScores(new HashMap<>())
                    .alerts(new ArrayList<>())
                    .recommendations(new ArrayList<>())
                    .build();
        }

        Map<MetricType, MetricScore> metricScores = new EnumMap<>(MetricType.class);
        List<String> alerts = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        double totalScore = 0;
        int scoreCount = 0;

        for (HealthMetric metric : recentMetrics) {
            MetricType type = metric.getMetricType();
            if (metric.getAlertTriggered() != null && metric.getAlertTriggered()) {
                alerts.add(metric.getAlertMessage());
            }

            double[] range = NORMAL_RANGES.get(type);
            if (range != null) {
                double score = calculateMetricScore(
                        metric.getValue(), range[0], range[1], range[2], range[3]);
                String status = getStatusLabel(score);
                metricScores.put(type, new MetricScore(type, metric.getValue(), score, status));
                totalScore += score;
                scoreCount++;

                if (score < 50) {
                    recommendations.add(getRecommendation(type, metric.getValue(), range));
                }
            }
        }

        double overallScore = scoreCount > 0 ? totalScore / scoreCount : 0;
        String overallStatus = getOverallStatus(overallScore);

        if (overallScore < 50) {
            overallStatus = "⚠️ CẦN CHÚ Ý";
            recommendations.add(0, "🚨 Nên đưa thú cưng đến bác sĩ thú y kiểm tra sớm!");
        } else if (overallScore >= 80) {
            overallStatus = "✅ TUYỆT VỜI";
            recommendations.add("🎉 Thú cưng của bạn rất khỏe mạnh! Tiếp tục duy trì chế độ chăm sóc tốt.");
        }

        return HealthAssessmentResult.builder()
                .petId(pet.getId())
                .petName(pet.getName())
                .overallScore(Math.round(overallScore * 10) / 10.0)
                .status(overallStatus)
                .message(generateSummaryMessage(overallScore, alerts.size(), recommendations.size()))
                .metricScores(metricScores)
                .alerts(alerts)
                .recommendations(recommendations)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    public HealthTrend analyzeTrend(Pet pet, MetricType type, int days) {
        int safeDays = Math.max(1, days);
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(safeDays);
        DateTimeFormatter chartTimeFmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");

        List<TrendPoint> points = collectTrendPoints(pet, type, start, end);
        int n = points.size();

        if (n < 2) {
            List<String> lbl = new ArrayList<>();
            List<Double> val = new ArrayList<>();
            for (TrendPoint p : points) {
                lbl.add(p.time().format(chartTimeFmt));
                val.add(p.value());
            }
            String hint = n == 0
                    ? (type == MetricType.WEIGHT
                            ? "Chưa có điểm cân nặng trong khoảng thời gian này. Ghi nhận cân tại mục Cân nặng của thú cưng hoặc chỉ số WEIGHT ở trang sức khỏe."
                            : "Chưa có chỉ số nào trong khoảng thời gian này. Hãy ghi nhận thêm trên trang chi tiết sức khỏe.")
                    : "Cần ít nhất 2 điểm dữ liệu để vẽ biểu đồ và phân tích xu hướng.";
            return HealthTrend.builder()
                    .type(type)
                    .trend("INSUFFICIENT_DATA")
                    .message(hint)
                    .changePercent(0.0)
                    .firstValue(n == 0 ? 0.0 : points.get(0).value())
                    .lastValue(n == 0 ? 0.0 : points.get(n - 1).value())
                    .average(points.stream().mapToDouble(TrendPoint::value).average().orElse(0))
                    .dataPoints(n)
                    .chartLabels(List.copyOf(lbl))
                    .chartValues(List.copyOf(val))
                    .build();
        }

        double first = points.get(0).value();
        double last = points.get(n - 1).value();
        double change = first != 0 ? ((last - first) / first) * 100 : 0;

        String trend;
        if (Math.abs(change) < 5) {
            trend = "STABLE";
        } else if (change > 0) {
            trend = "INCREASING";
        } else {
            trend = "DECREASING";
        }

        String message = generateTrendMessage(type, trend, change, last);

        List<String> chartLabels = points.stream()
                .map(p -> p.time().format(chartTimeFmt))
                .toList();
        List<Double> chartValues = points.stream()
                .map(TrendPoint::value)
                .toList();

        return HealthTrend.builder()
                .type(type)
                .trend(trend)
                .message(message)
                .changePercent(Math.round(change * 10) / 10.0)
                .firstValue(first)
                .lastValue(last)
                .average(points.stream().mapToDouble(TrendPoint::value).average().orElse(0))
                .dataPoints(n)
                .chartLabels(chartLabels)
                .chartValues(chartValues)
                .build();
    }

    private List<TrendPoint> collectTrendPoints(Pet pet, MetricType type, LocalDateTime start, LocalDateTime end) {
        List<TrendPoint> out = new ArrayList<>();
        if (type == MetricType.WEIGHT) {
            LocalDate startDate = start.toLocalDate();
            for (WeightRecord w : weightRecordRepository.findByPetIdAndRecordDateGreaterThanEqualOrderByRecordDateAsc(
                    pet.getId(), startDate)) {
                double kg = parseWeightKg(w.getWeight());
                if (!Double.isNaN(kg)) {
                    out.add(new TrendPoint(w.getRecordDate().atStartOfDay(), kg));
                }
            }
        }
        for (HealthMetric m : healthMetricRepository.findByPetAndTypeInDateRange(pet.getId(), type, start, end)) {
            out.add(new TrendPoint(m.getRecordedAt(), m.getValue()));
        }
        out.sort(Comparator.comparing(TrendPoint::time));
        return out;
    }

    private static double parseWeightKg(String raw) {
        if (raw == null || raw.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private record TrendPoint(LocalDateTime time, double value) {}

    public NutritionAdvice generateNutritionAdvice(Pet pet, List<HealthMetric> recentMetrics) {
        double avgWeight = recentMetrics.stream()
                .filter(m -> m.getMetricType() == MetricType.WEIGHT)
                .mapToDouble(HealthMetric::getValue)
                .average().orElse(0);

        double avgAppetite = recentMetrics.stream()
                .filter(m -> m.getMetricType() == MetricType.APPETITE)
                .mapToDouble(HealthMetric::getValue)
                .average().orElse(5.0);

        double avgActivity = recentMetrics.stream()
                .filter(m -> m.getMetricType() == MetricType.ACTIVITY)
                .mapToDouble(HealthMetric::getValue)
                .average().orElse(5.0);

        String category = pet.getCategory() != null ? pet.getCategory().getName() : "Thú cưng";
        double idealCalories = calculateIdealCalories(category, avgWeight, avgActivity);

        List<String> foods = getRecommendedFoods(category, avgWeight, avgActivity);

        return NutritionAdvice.builder()
                .petId(pet.getId())
                .petName(pet.getName())
                .currentWeight(Math.round(avgWeight * 100.0) / 100.0)
                .idealWeight(avgWeight)
                .dailyCalories(Math.round(idealCalories))
                .mealPlan(generateMealPlan(avgWeight, idealCalories))
                .recommendedFoods(foods)
                .foodsToAvoid(getFoodsToAvoid(category))
                .supplements(getRecommendedSupplements(category, avgActivity))
                .hydrationAdvice(getHydrationAdvice(avgWeight))
                .tips(getNutritionTips(category))
                .build();
    }

    private double calculateMetricScore(double value, double minNormal, double maxNormal,
                                       double critLow, double critHigh) {
        if (value >= minNormal && value <= maxNormal) {
            return 100;
        }

        if (value >= critLow && value <= critHigh) {
            if (value < minNormal) {
                double range = minNormal - critLow;
                double deviation = minNormal - value;
                return Math.max(50, 100 - (deviation / range) * 50);
            } else {
                double range = critHigh - maxNormal;
                double deviation = value - maxNormal;
                return Math.max(50, 100 - (deviation / range) * 50);
            }
        }

        if (value < critLow) {
            return Math.max(0, 30 - (critLow - value) * 5);
        } else {
            return Math.max(0, 30 - (value - critHigh) * 2);
        }
    }

    private String getStatusLabel(double score) {
        if (score >= 90) return "🟢 Tốt";
        if (score >= 70) return "🟡 Bình thường";
        if (score >= 50) return "🟠 Cần theo dõi";
        return "🔴 Cần can thiệp";
    }

    private String getOverallStatus(double score) {
        if (score >= 85) return "✅ KHỎE MẠNH";
        if (score >= 70) return "🟡 BÌNH THƯỜNG";
        if (score >= 50) return "🟠 CẦN CHÚ Ý";
        return "🔴 CẦN KIỂM TRA";
    }

    private String getMetricLabel(MetricType type) {
        return switch (type) {
            case WEIGHT -> "Cân nặng";
            case HEIGHT -> "Chiều cao";
            case TEMPERATURE -> "Nhiệt độ";
            case HEART_RATE -> "Nhịp tim";
            case RESPIRATION -> "Nhịp thở";
            case APPETITE -> "Ăn uống";
            case ACTIVITY -> "Hoạt động";
            case SLEEP_QUALITY -> "Giấc ngủ";
            case WATER_INTAKE -> "Nước uống";
            case BOWEL_MOVEMENT -> "Đi vệ sinh";
            case SEIZURE_COUNT -> "Số cơn động kinh";
            case SEIZURE_DURATION -> "Thời gian cơn";
            case SEIZURE_SEVERITY -> "Mức độ cơn";
            case MEDICATION_DOSE -> "Liều thuốc";
            case BLOOD_PRESSURE -> "Huyết áp";
            case BLOOD_SUGAR -> "Đường huyết";
        };
    }

    private String getAdvice(MetricType type, double value, double min, double max) {
        if (value >= min && value <= max) {
            return "✅ Giá trị nằm trong ngưỡng bình thường.";
        }
        if (value < min) {
            return String.format("📉 Giá trị thấp hơn bình thường (bình thường: %.1f - %.1f).", min, max);
        }
        return String.format("📈 Giá trị cao hơn bình thường (bình thường: %.1f - %.1f).", min, max);
    }

    private String getRecommendation(MetricType type, double value, double[] range) {
        return switch (type) {
            case WEIGHT -> value < range[0]
                    ? "🐾 Cân nặng giảm - có thể do dinh dưỡng kém hoặc bệnh lý. Nên đi khám."
                    : "🐾 Cân nặng tăng - cần điều chỉnh chế độ ăn và tăng vận động.";
            case TEMPERATURE -> "🌡️ Nhiệt độ bất thường - có thể có sốt hoặc hạ thân nhiệt. Theo dõi và đi khám nếu kéo dài.";
            case HEART_RATE -> "❤️ Nhịp tim bất thường - cần được bác sĩ thú y kiểm tra.";
            case APPETITE -> "🍽️ Ăn uống kém - có thể do bệnh hoặc stress. Theo dõi và điều chỉnh thức ăn.";
            case SEIZURE_COUNT -> "⚠️ Xuất hiện cơn động kinh - cần gặp bác sĩ thú y ngay để được tư vấn điều trị.";
            case WATER_INTAKE -> "💧 Uống nước ít/ nhiều bất thường - theo dõi và đi khám nếu kéo dài.";
            default -> String.format("📊 Theo dõi chỉ số %s. Nếu không cải thiện, hãy hỏi bác sĩ thú y.", type.name());
        };
    }

    private String generateSummaryMessage(double score, int alertCount, int recCount) {
        if (score >= 85) {
            return String.format("Thú cưng của bạn đang rất khỏe mạnh! %d chỉ số trong ngưỡng tốt.",
                    recCount == 0 ? metricCountForScore(score) : 0);
        } else if (score >= 70) {
            return String.format("Sức khỏe tổng quát ở mức bình thường. Có %d điều cần theo dõi.",
                    recCount);
        } else if (score >= 50) {
            return String.format("Có %d vấn đề cần chú ý. %s", recCount,
                    alertCount > 0 ? "Đặc biệt có " + alertCount + " cảnh báo cần xử lý ngay!" : "Nên hỏi bác sĩ thú y.");
        }
        return String.format("⚠️ Phát hiện %d cảnh báo và %d vấn đề cần theo dõi. Hãy đưa thú cưng đến bác sĩ thú y ngay!",
                alertCount, recCount);
    }

    private String generateTrendMessage(MetricType type, String trend, double change, double lastValue) {
        String label = getMetricLabel(type);
        String trendIcon = switch (trend) {
            case "INCREASING" -> "📈";
            case "DECREASING" -> "📉";
            default -> "➡️";
        };
        String status = switch (trend) {
            case "STABLE" -> "khá ổn định";
            case "INCREASING" -> type == MetricType.WEIGHT ? "có xu hướng tăng" : "tăng";
            case "DECREASING" -> type == MetricType.WEIGHT ? "có xu hướng giảm" : "giảm";
            default -> "không xác định";
        };
        return String.format("%s %s: %s (thay đổi %.1f%%, giá trị gần nhất: %.1f)", trendIcon, label, status, change, lastValue);
    }

    private double calculateIdealCalories(String category, double weight, double activity) {
        double baseCalories = switch (category.toLowerCase()) {
            case "chó", "dog" -> weight * 30 + 70;
            case "mèo", "cat" -> weight * 30 + 70;
            default -> weight * 30 + 70;
        };
        double activityFactor = 1.0 + (activity - 5) * 0.05;
        return baseCalories * activityFactor;
    }

    private List<String> getRecommendedFoods(String category, double weight, double activity) {
        List<String> foods = new ArrayList<>();
        if (weight > 15) {
            foods.add("Thức ăn giảm béo chuyên dụng");
            foods.add("Thịt nạc ít mỡ");
        } else {
            foods.add("Thức ăn giàu protein chất lượng cao");
        }
        foods.add("Rau xanh hấp (bông cải xanh, cà rốt)");
        foods.add("Nước luộc thịt không gia vị");
        return foods;
    }

    private List<String> getFoodsToAvoid(String category) {
        return List.of(
                "Chocolate (gây độc cho chó/mèo)",
                "Hành, tỏi (gây thiếu máu)",
                "Nho, nho khô (gây suy thận)",
                "Xương gà, xương nhỏ (dễ hóc)",
                "Thức ăn quá mặn, nhiều gia vị",
                "Bia, rượu, caffeine"
        );
    }

    private List<String> getRecommendedSupplements(String category, double activity) {
        List<String> supplements = new ArrayList<>();
        supplements.add("Dầu cá Omega-3 (tốt cho lông và da)");
        supplements.add("Glucosamine (hỗ trợ khớp)");
        if (activity < 5) {
            supplements.add("Vitamin B complex (tăng năng lượng)");
        }
        return supplements;
    }

    private String getHydrationAdvice(double weight) {
        double idealWater = weight * 50;
        return String.format("Nên uống khoảng %.0f-%.0f ml nước mỗi ngày. Đặt nhiều chén nước ở nơi dễ thấy.",
                idealWater * 0.8, idealWater * 1.2);
    }

    private List<String> getNutritionTips(String category) {
        return List.of(
                "Chia thức ăn thành 2-3 bữa/ngày thay vì 1 bữa lớn",
                "Đo lường lượng thức ăn để kiểm soát cân nặng",
                "Thay đổi thức ăn từ từ trong 7-10 ngày để tránh rối loạn tiêu hóa",
                "Theo dõi cân nặng hàng tuần và điều chỉnh khẩu phần",
                "Luôn có nước sạch available"
        );
    }

    private String generateMealPlan(double weight, double calories) {
        int meals = weight > 20 ? 2 : 3;
        double perMeal = calories / meals;
        StringBuilder plan = new StringBuilder();
        for (int i = 1; i <= meals; i++) {
            plan.append(String.format("Bữa %d: ~%.0f kcal\n", i, perMeal));
        }
        return plan.toString();
    }

    private int metricCountForScore(double score) {
        return score >= 95 ? 5 : score >= 90 ? 4 : 3;
    }

    @lombok.Value
    @lombok.Builder
    public static class HealthAssessmentResult {
        Long petId;
        String petName;
        double overallScore;
        String status;
        String message;
        Map<MetricType, MetricScore> metricScores;
        List<String> alerts;
        List<String> recommendations;
        LocalDateTime lastUpdated;
    }

    @lombok.Value
    public static class MetricScore {
        MetricType type;
        double value;
        double score;
        String status;
    }

    @lombok.Value
    @lombok.Builder
    public static class HealthTrend {
        MetricType type;
        String trend;
        String message;
        double changePercent;
        double firstValue;
        double lastValue;
        double average;
        int dataPoints;
        /** Nhãn trục X (thời gian ghi nhận) — đồng bộ với chartValues */
        List<String> chartLabels;
        /** Giá trị thực từ DB — đồng bộ với chartLabels */
        List<Double> chartValues;
    }

    /**
     * Gợi ý văn bản ngắn cho chat AI (không thay thế khám thú cưng).
     * Sử dụng NLP đơn giản để phân tích và trả lời theo ngữ cảnh.
     * Tránh trả lời lặp lại bằng cách lưu lịch sử hội thoại.
     */
    private final Map<String, List<String>> conversationHistory = new java.util.concurrent.ConcurrentHashMap<>();

    public String assess(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Bạn vui lòng nhập câu hỏi nhé.";
        }

        // Tạo session ID tạm (có thể cải thiện bằng session thực)
        String sessionId = "default";

        // Lấy lịch sử hội thoại
        List<String> history = conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());

        String lowerMsg = userMessage.toLowerCase().trim();
        String response;

        // Kiểm tra câu hỏi trùng lặp gần đây
        boolean isDuplicate = history.stream()
                .anyMatch(h -> h.toLowerCase().equals(lowerMsg));

        if (isDuplicate && history.size() > 1) {
            return "Mình đã trả lời câu hỏi này rồi nha! Bạn có thể hỏi câu khác hoặc liên hệ bác sĩ thú y tại <a href=\"/vet-qa\">đây</a> để được tư vấn chi tiết hơn nhé.";
        }

        // Phân tích và trả lời theo chủ đề
        response = analyzeAndRespond(lowerMsg, history);

        // Lưu vào lịch sử (giới hạn 10 tin nhắn gần nhất)
        if (history.size() >= 10) {
            history.remove(0);
        }
        history.add(userMessage);

        return response;
    }

    private String analyzeAndRespond(String message, List<String> history) {
        // ── CHÀO HỎI ──────────────────────────────────────────────
        if (message.matches(".*\\b(chào|xin chao|hello|hi|hey|chao|hola|alo)\\b.*")) {
            return "Chào bạn! 😺 Mình là <strong>Nekomimi AI</strong>, trợ lý chăm sóc thú cưng của Pet Care. " +
                    "Mình có thể giúp bạn về:<br>" +
                    "• <strong>Chăm sóc thú cưng</strong>: dinh dưỡng, vệ sinh, tập luyện<br>" +
                    "• <strong>Theo dõi sức khỏe</strong>: cân nặng, tiêm phòng, bệnh thường gặp<br>" +
                    "• <strong>Tìm dịch vụ</strong>: bác sĩ thú y, lịch hẹn, tìm thú lạc<br>" +
                    "Bạn cần hỗ trợ gì hôm nay?";
        }

        // ── TRIỆU CHỨNG BỆNH ───────────────────────────────────────
        if (message.matches(".*\\b(ho|ho|viem|viêm|nhiet|run|la|bieng an|chan dong|bung tieu chay|non|non|oi|thở|tho)\\b.*") ||
            message.matches(".*\\b(khong an|chán ăn|ỉa|đi ngoài|biếng ăn|lười|bỏ ăn)\\b.*")) {
            return "⚠️ Về triệu chứng bạn mô tả:<br><br>" +
                    "<strong>Khuyến nghị tạm thời:</strong><br>" +
                    "• Theo dõi kỹ thú cưng trong 24-48 giờ<br>" +
                    "• Đảm bảo nước uống sạch và đầy đủ<br>" +
                    "• Nếu triệu chứng kéo dài hoặc nặng hơn, hãy đưa bé đi khám<br><br>" +
                    "📞 <strong>Cấp cứu:</strong> Gọi 1900 6750 nếu thú cưng:<br>" +
                    "• Khó thở nặng, ho ra máu<br>" +
                    "• Nôn liên tục không ngừng<br>" +
                    "• Mất ý thức hoặc co giật<br><br>" +
                    "Bạn có thể <a href=\"/vet-qa\">hỏi bác sĩ thú y</a> để được tư vấn cụ thể hơn nhé!";
        }

        // ── DINH DƯỠNG ────────────────────────────────────────────
        if (message.matches(".*\\b(an|uong|thuc an|che bien|dua|ga|thit|ca|gao|bot|sua|vitamin)\\b.*") ||
            message.matches(".*\\b(cho an gi|meo an gi|thu cung an gi|dinh duong|bu che)\\b.*")) {
            return "🍽️ Về dinh dưỡng cho thú cưng:<br><br>" +
                    "<strong>Nguyên tắc chung:</strong><br>" +
                    "• Chó: 2 bữa/ngày, khoảng 2-3% trọng lượng cơ thể<br>" +
                    "• Mèo: 2-3 bữa nhỏ/ngày, có thể để thức ăn tự do<br><br>" +
                    "<strong>Thức ăn tốt:</strong><br>" +
                    "• Thịt nạc nấu chín (gà, bò, cá)<br>" +
                    "• Rau củ hấp (cà rốt, bí đỏ, bông cải)<br>" +
                    "• Thức ăn hạt chuyên dụng cho từng loài<br><br>" +
                    "<strong>Tuyệt đối tránh:</strong><br>" +
                    "• Chocolate, hành/tỏi, nho, caffeine<br>" +
                    "• Xương nhỏ, thức ăn quá mặn hoặc nhiều gia vị<br><br>" +
                    "Bạn muốn biết thêm về chế độ ăn cụ thể cho bé không?";
        }

        // ── TIÊM PHÒNG ────────────────────────────────────────────
        if (message.matches(".*\\b(tiem|tiêm|vacxin|vaccine|phan|phòng|sot|dại|care|parvo)\\b.*")) {
            return "💉 Về tiêm phòng:<br><br>" +
                    "<strong>Lịch tiêm cơ bản cho chó:</strong><br>" +
                    "• 6-8 tuần: Vaccine 5 bệnh (care, parvo, viêm gan...)<br>" +
                    "• 9-11 tuần: Mũi nhắc lại 1<br>" +
                    "• 12-14 tuần: Mũi nhắc lại 2 + Vaccine dại<br>" +
                    "• Hàng năm: Nhắc lại hàng năm<br><br>" +
                    "<strong>Lịch tiêm cho mèo:</strong><br>" +
                    "• 8-9 tuần: Vaccine 3 bệnh (dịch mèo, viêm đường hô hấp...)<br>" +
                    "• 12 tuần: Nhắc lại + Vaccine dại<br>" +
                    "• Hàng năm: Nhắc lại<br><br>" +
                    "Bạn có thể <a href=\"/pets\">kiểm tra lịch tiêm</a> của thú cưng trong hồ sơ nhé!";
        }

        // ── TÌM BÁC SĨ ─────────────────────────────────────────────
        if (message.matches(".*\\b(bac si|bác sĩ|thú y|vet|dich vu|khám|phòng khám)\\b.*")) {
            return "🔍 Tìm bác sĩ thú y:<br><br>" +
                    "Bạn có thể:<br>" +
                    "• Xem danh sách bác sĩ và chat riêng <a href=\"/vet-qa\">tại đây</a><br>" +
                    "• Đặt lịch hẹn khám <a href=\"/appointments\">tại đây</a><br><br>" +
                    "<strong>Lưu ý:</strong> Với các vấn đề nghiêm trọng, hãy đưa thú cưng đến phòng khám gần nhất thay vì chỉ tư vấn online nhé!";
        }

        // ── THÚ LẠC ────────────────────────────────────────────────
        if (message.matches(".*\\b(lac|thất lạc|lost|tim thu|mat|đi lạc|con thú|mất)\\b.*")) {
            return "🔎 Tìm thú cưng lạc:<br><br>" +
                    "Bạn có thể:<br>" +
                    "• Đăng tin tìm kiếm <a href=\"/lost-pets\">tại đây</a><br>" +
                    "• Xem các báo cáo thú lạc gần đây<br>" +
                    "• Chia sẻ thông tin để được nhiều người giúp đỡ<br><br>" +
                    "<strong>Mẹo tìm nhanh:</strong><br>" +
                    "• Gọi tên bé thật nhiều<br>" +
                    "• Để đồ có mùi quen thuộc của bé ở nơi bé hay đi<br>" +
                    "• Thông báo cho chính quyền địa phương và cơ sở cứu hộ";
        }

        // ── CÂN NẶNG ───────────────────────────────────────────────
        if (message.matches(".*\\b(can nang|weight|gay|beo|gầy|người mẫu|béo phì)\\b.*")) {
            return "⚖️ Về cân nặng thú cưng:<br><br>" +
                    "<strong>Chó - Cân nặng lý tưởng theo giống:</strong><br>" +
                    "• Chihuahua: 1-3 kg<br>" +
                    "• Poodle: 3-7 kg<br>" +
                    "• Husky: 18-27 kg<br>" +
                    "• Golden Retriever: 25-35 kg<br><br>" +
                    "<strong>Mèo - Cân nặng lý tưởng:</strong><br>" +
                    "• Mèo nhà: 3-5 kg<br>" +
                    "• Mèo Anh lông ngắn: 4-8 kg<br>" +
                    "• Maine Coon: 5-11 kg<br><br>" +
                    "Bạn có thể <a href=\"/pets\">theo dõi cân nặng</a> của bé tại trang thú cưng nhé!";
        }

        // ── HÀNH VI ────────────────────────────────────────────────
        if (message.matches(".*\\b(hành vi|hanh vi|nghich|canh|keu|gau|meo|đào|can|grumpy|cắn|sợ)\\b.*")) {
            return "🧠 Về hành vi thú cưng:<br><br>" +
                    "<strong>Một số hành vi thường gặp:</strong><br>" +
                    "• Gầu/gừ: Đánh dấu lãnh thổ, cần triệt sản<br>" +
                    "• Cắn: Có thể do sợ hãi, đau, hoặc chơi quá trớn<br>" +
                    "• Liếm: Biểu hiện thân thiện hoặc tự làm sạch<br>" +
                    "• Kêu nhiều: Có thể đói, buồn, hoặc muốn ra ngoài<br><br>" +
                    "<strong>Mẹo điều chỉnh:</strong><br>" +
                    "• Thưởng khi bé làm tốt, không phạt khi làm sai<br>" +
                    "• Đảm bảo đủ vận động và chơi mỗi ngày<br>" +
                    "• Tham khảo huấn luyện viên chuyên nghiệp nếu cần";
        }

        // ── VỆ SINH ────────────────────────────────────────────────
        if (message.matches(".*\\b(tam|chải lông|cắt móng|tắm|rửa|vệ sinh|sạch|hygiene)\\b.*")) {
            return "🛁 Về vệ sinh thú cưng:<br><br>" +
                    "<strong>Tắm rửa:</strong><br>" +
                    "• Chó: 1-2 tuần/lần (tùy giống)<br>" +
                    "• Mèo: Thường tự làm sạch, có thể tắm 1-2 tháng/lần<br>" +
                    "• Dùng sữa tắm chuyên dụng cho thú cưng<br><br>" +
                    "<strong>Chải lông:</strong><br>" +
                    "• Chải hàng ngày với giống lông dài<br>" +
                    "• Chải 2-3 lần/tuần với giống lông ngắn<br><br>" +
                    "<strong>Cắt móng:</strong><br>" +
                    "• Cắt 2-4 tuần/lần<br>" +
                    "• Cẩn thận không cắt vào phần mạch máu (móng hồng)";
        }

        // ── LỊCH HẸN ───────────────────────────────────────────────
        if (message.matches(".*\\b(lich|đặt|hẹn|hen|lịch|appointment|calendar|nano)\\b.*")) {
            return "📅 Đặt lịch hẹn:<br><br>" +
                    "Bạn có thể đặt lịch hẹn khám tại <a href=\"/appointments\">đây</a><br><br>" +
                    "<strong>Chuẩn bị trước khi khám:</strong><br>" +
                    "• Mang theo sổ tiêm phòng (nếu có)<br>" +
                    "• Liệt kê các triệu chứng bất thường<br>" +
                    "• Nhịn ăn 8-12 tiếng nếu cần xét nghiệm<br>" +
                    "• Chuẩn bị câu hỏi muốn hỏi bác sĩ";
        }

        // ── CẢM ƠN / KẾT THÚC ─────────────────────────────────────
        if (message.matches(".*\\b(cam on|cảm ơn|thank|thanks|ok|okay|tạm biệt|bye)\\b.*")) {
            return "Cảm ơn bạn đã trò chuyện! 😸<br><br>" +
                    "Nếu cần thêm hỗ trợ, đừng ngần ngại:<br>" +
                    "• <a href=\"/vet-qa\">Hỏi bác sĩ thú y</a><br>" +
                    "• <a href=\"/pets\">Quản lý thú cưng</a><br>" +
                    "• Hotline: <strong>1900 6750</strong><br><br>" +
                    "Chúc bạn và thú cưng luôn khỏe mạnh! 🐾";
        }

        // ── MẶC ĐỊNH ───────────────────────────────────────────────
        return "Cảm ơn bạn đã nhắn! 🐾<br><br>" +
                "Mình là <strong>Nekomimi AI</strong> - trợ lý chăm sóc thú cưng.<br>" +
                "Mình có thể hỗ trợ về:<br>" +
                "• Sức khỏe & dinh dưỡng<br>" +
                "• Tiêm phòng & bệnh thường gặp<br>" +
                "• Hành vi & huấn luyện<br>" +
                "• Tìm dịch vụ thú y<br><br>" +
                "Bạn có thể <a href=\"/vet-qa\">hỏi bác sĩ thú y</a> để được tư vấn chi tiết và chính xác hơn nhé!<br>" +
                "📞 Hotline: <strong>1900 6750</strong>";
    }

    @lombok.Value
    @lombok.Builder
    public static class NutritionAdvice {
        Long petId;
        String petName;
        double currentWeight;
        double idealWeight;
        double dailyCalories;
        String mealPlan;
        List<String> recommendedFoods;
        List<String> foodsToAvoid;
        List<String> supplements;
        String hydrationAdvice;
        List<String> tips;
    }
}
