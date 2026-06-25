package swp391.carwash.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import swp391.carwash.repository.BookingBehaviorMonthlyView;

@Component
public class AiInsightRuleEngine {
    static final String MODEL_VERSION = "rule-v1";
    static final String CUSTOMER_SEGMENT = "CUSTOMER_SEGMENT";
    static final String CHURN_RISK = "CHURN_RISK";
    static final String PROMOTION_RECOMMENDATION = "PROMOTION_RECOMMENDATION";
    static final String SERVICE_RECOMMENDATION = "SERVICE_RECOMMENDATION";

    private static final BigDecimal VIP_SPEND_THRESHOLD = new BigDecimal("1000000");
    private static final BigDecimal HIGH_SPEND_THRESHOLD = new BigDecimal("700000");

    public List<InsightDraft> evaluate(BookingBehaviorMonthlyView behavior) {
        String segment = segmentLabel(behavior);
        String churnRisk = churnRiskLabel(behavior);
        return List.of(
                customerSegment(behavior, segment),
                churnRisk(behavior, churnRisk),
                promotionRecommendation(behavior, segment, churnRisk),
                serviceRecommendation(behavior)
        );
    }

    private InsightDraft customerSegment(BookingBehaviorMonthlyView behavior, String segment) {
        return new InsightDraft(
                CUSTOMER_SEGMENT,
                segment,
                segmentReason(segment),
                promotionAction(segment, churnRiskLabel(behavior)),
                confidenceForSegment(segment),
                payload(behavior)
        );
    }

    private InsightDraft churnRisk(BookingBehaviorMonthlyView behavior, String churnRisk) {
        return new InsightDraft(
                CHURN_RISK,
                churnRisk,
                churnReason(churnRisk),
                promotionAction(segmentLabel(behavior), churnRisk),
                confidenceForChurn(churnRisk),
                payload(behavior)
        );
    }

    private InsightDraft promotionRecommendation(BookingBehaviorMonthlyView behavior, String segment, String churnRisk) {
        String action = promotionAction(segment, churnRisk);
        return new InsightDraft(
                PROMOTION_RECOMMENDATION,
                promotionLabel(segment, churnRisk),
                "Đề xuất ưu đãi dựa trên phân nhóm khách hàng và nguy cơ rời bỏ.",
                action,
                recommendationConfidence(segment, churnRisk),
                payload(behavior)
        );
    }

    private InsightDraft serviceRecommendation(BookingBehaviorMonthlyView behavior) {
        String label;
        String action;
        BigDecimal totalSpent = money(behavior.getTotalSpent());
        if (totalSpent.compareTo(HIGH_SPEND_THRESHOLD) >= 0) {
            label = "PREMIUM_UPSELL";
            action = "Gợi ý gói premium/detailing cho khách có mức chi tiêu cao.";
        } else if (number(behavior.getTotalBookings()) <= 1) {
            label = "BASIC_EXPERIENCE";
            action = "Gợi ý gói cơ bản hoặc combo trải nghiệm để tăng lần quay lại.";
        } else if (behavior.getPreferredSlotId() != null) {
            label = "PREFERRED_SLOT_REBOOK";
            action = "Gợi ý đặt lại ở khung giờ khách thường dùng.";
        } else {
            label = "STANDARD_SERVICE";
            action = "Gợi ý combo định kỳ phù hợp với lịch sử sử dụng dịch vụ.";
        }
        return new InsightDraft(
                SERVICE_RECOMMENDATION,
                label,
                "Đề xuất dịch vụ dựa trên chi tiêu, tần suất đặt lịch và khung giờ yêu thích.",
                action,
                "STANDARD_SERVICE".equals(label) ? confidence("0.60") : confidence("0.75"),
                payload(behavior)
        );
    }

    private String segmentLabel(BookingBehaviorMonthlyView behavior) {
        int cancelledOrNoShow = number(behavior.getCancelledCount()) + number(behavior.getNoShowCount());
        if (cancelledOrNoShow >= 2) {
            return "AT_RISK";
        }
        if (number(behavior.getCompletedCount()) >= 5 || money(behavior.getTotalSpent()).compareTo(VIP_SPEND_THRESHOLD) >= 0) {
            return "VIP";
        }
        if (number(behavior.getCompletedCount()) >= 3
                && number(behavior.getCancelledCount()) == 0
                && number(behavior.getNoShowCount()) == 0) {
            return "LOYAL";
        }
        if (number(behavior.getTotalBookings()) <= 1) {
            return "NEW";
        }
        return "REGULAR";
    }

    private String churnRiskLabel(BookingBehaviorMonthlyView behavior) {
        int cancelledOrNoShow = number(behavior.getCancelledCount()) + number(behavior.getNoShowCount());
        if (cancelledOrNoShow >= 2) {
            return "HIGH";
        }
        if (cancelledOrNoShow == 1 || number(behavior.getCompletedCount()) == 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String segmentReason(String segment) {
        return switch (segment) {
            case "AT_RISK" -> "Khách có tỷ lệ hủy/no-show cao trong kỳ.";
            case "VIP" -> "Khách có số lần hoàn thành hoặc tổng chi tiêu cao.";
            case "LOYAL" -> "Khách hoàn thành nhiều booking và không có hủy/no-show.";
            case "NEW" -> "Khách mới hoặc mới có rất ít booking.";
            default -> "Khách có hành vi sử dụng ổn định nhưng chưa thuộc nhóm nổi bật.";
        };
    }

    private String churnReason(String churnRisk) {
        return switch (churnRisk) {
            case "HIGH" -> "Nguy cơ rời bỏ cao vì khách có nhiều lần hủy hoặc no-show.";
            case "MEDIUM" -> "Nguy cơ rời bỏ trung bình vì có dấu hiệu hủy/no-show hoặc chưa hoàn thành booking.";
            default -> "Nguy cơ rời bỏ thấp vì khách vẫn hoàn thành booking đều.";
        };
    }

    private String promotionAction(String segment, String churnRisk) {
        if ("AT_RISK".equals(segment) || "HIGH".equals(churnRisk)) {
            return "Gửi voucher quay lại 15% kèm nhắc lịch trước 24 giờ.";
        }
        if ("VIP".equals(segment)) {
            return "Tặng ưu đãi loyalty hoặc ưu tiên khung giờ đẹp.";
        }
        if ("NEW".equals(segment)) {
            return "Gửi ưu đãi lần rửa tiếp theo để tăng quay lại.";
        }
        return "Gợi ý combo định kỳ hoặc nhắc lịch theo khung giờ yêu thích.";
    }

    private String promotionLabel(String segment, String churnRisk) {
        if ("AT_RISK".equals(segment) || "HIGH".equals(churnRisk)) {
            return "WIN_BACK_VOUCHER";
        }
        if ("VIP".equals(segment)) {
            return "LOYALTY_REWARD";
        }
        if ("NEW".equals(segment)) {
            return "SECOND_VISIT_OFFER";
        }
        return "PERIODIC_COMBO";
    }

    private BigDecimal confidenceForSegment(String segment) {
        return switch (segment) {
            case "AT_RISK", "VIP" -> confidence("0.90");
            case "LOYAL" -> confidence("0.75");
            default -> confidence("0.60");
        };
    }

    private BigDecimal confidenceForChurn(String churnRisk) {
        return switch (churnRisk) {
            case "HIGH" -> confidence("0.90");
            case "MEDIUM" -> confidence("0.75");
            default -> confidence("0.60");
        };
    }

    private BigDecimal recommendationConfidence(String segment, String churnRisk) {
        if ("AT_RISK".equals(segment) || "HIGH".equals(churnRisk) || "VIP".equals(segment)) {
            return confidence("0.90");
        }
        if ("LOYAL".equals(segment) || "MEDIUM".equals(churnRisk)) {
            return confidence("0.75");
        }
        return confidence("0.60");
    }

    private Map<String, Object> payload(BookingBehaviorMonthlyView behavior) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalBookings", number(behavior.getTotalBookings()));
        metrics.put("completedCount", number(behavior.getCompletedCount()));
        metrics.put("cancelledCount", number(behavior.getCancelledCount()));
        metrics.put("noShowCount", number(behavior.getNoShowCount()));
        metrics.put("totalSpent", money(behavior.getTotalSpent()));
        metrics.put("preferredSlotId", behavior.getPreferredSlotId());
        return metrics;
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal confidence(String value) {
        return new BigDecimal(value);
    }

    public record InsightDraft(
            String insightType,
            String label,
            String reason,
            String recommendedAction,
            BigDecimal confidenceScore,
            Map<String, Object> metrics
    ) {
        Map<String, Object> predictionValue() {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("label", label);
            value.put("reason", reason);
            value.put("recommendedAction", recommendedAction);
            value.put("metrics", metrics);
            return value;
        }
    }
}
