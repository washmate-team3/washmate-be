package swp391.carwash.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import swp391.carwash.entity.CustomerAiInsight;

public record CustomerAiInsightResponse(
        Integer insightId,
        Integer garageId,
        String garageName,
        String period,
        String insightType,
        Map<String, Object> predictionValue,
        BigDecimal confidenceScore,
        String modelVersion,
        OffsetDateTime generatedAt
) {
    public static CustomerAiInsightResponse from(CustomerAiInsight insight) {
        return new CustomerAiInsightResponse(
                insight.getId(),
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
