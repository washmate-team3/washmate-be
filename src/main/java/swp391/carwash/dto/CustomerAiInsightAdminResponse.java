package swp391.carwash.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import swp391.carwash.entity.CustomerAiInsight;

public record CustomerAiInsightAdminResponse(
        Integer insightId,
        Integer userId,
        String customerName,
        Integer garageId,
        String garageName,
        String period,
        String insightType,
        Map<String, Object> predictionValue,
        BigDecimal confidenceScore,
        String modelVersion,
        OffsetDateTime generatedAt
) {
    public static CustomerAiInsightAdminResponse from(CustomerAiInsight insight) {
        return new CustomerAiInsightAdminResponse(
                insight.getId(),
                insight.getUser().getId(),
                insight.getUser().getFullName(),
                insight.getGarage().getId(),
                insight.getGarage().getName(),
                insight.getPeriod(),
                insight.getInsightType(),
                insight.getPredictionValue(),
                insight.getConfidenceScore(),
                insight.getModelVersion(),
                insight.getGeneratedAt()
        );
    }
}
