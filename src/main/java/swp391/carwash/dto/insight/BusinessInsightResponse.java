package swp391.carwash.dto.insight;

import java.time.OffsetDateTime;
import swp391.carwash.entity.BusinessInsight;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightStatus;
import swp391.carwash.enums.InsightType;

public record BusinessInsightResponse(
        Integer id,
        String ruleCode,
        InsightType type,
        InsightSeverity severity,
        String title,
        String summary,
        String evidence,
        String meaning,
        String recommendation,
        String relatedMetric,
        InsightStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        AIInsightEnrichResponse aiEnrichment
) {
    public static BusinessInsightResponse from(BusinessInsight insight) {
        return from(insight, null);
    }

    public static BusinessInsightResponse from(BusinessInsight insight, AIInsightEnrichResponse aiEnrichment) {
        return new BusinessInsightResponse(
                insight.getId(),
                insight.getRuleCode(),
                insight.getType(),
                insight.getSeverity(),
                insight.getTitle(),
                insight.getSummary(),
                insight.getEvidence(),
                insight.getMeaning(),
                insight.getRecommendation(),
                insight.getRelatedMetric(),
                insight.getStatus(),
                insight.getCreatedAt(),
                insight.getUpdatedAt(),
                aiEnrichment);
    }
}
