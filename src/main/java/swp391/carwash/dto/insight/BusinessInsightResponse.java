package swp391.carwash.dto.insight;

import java.time.OffsetDateTime;
import swp391.carwash.entity.BusinessInsight;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightSource;
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
        InsightSource source,
        Boolean verified,
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
                sourceOf(insight, aiEnrichment),
                verifiedOf(aiEnrichment),
                insight.getCreatedAt(),
                insight.getUpdatedAt(),
                aiEnrichment);
    }

    private static InsightSource sourceOf(BusinessInsight insight, AIInsightEnrichResponse aiEnrichment) {
        if (aiEnrichment != null && aiEnrichment.getSource() != null) {
            return aiEnrichment.getSource();
        }
        return insight.getRuleCode() != null && insight.getRuleCode().startsWith("AI_")
                ? InsightSource.AI_DETECTED
                : InsightSource.RULE_BASED;
    }

    private static Boolean verifiedOf(AIInsightEnrichResponse aiEnrichment) {
        return aiEnrichment == null || aiEnrichment.getVerified() == null ? Boolean.TRUE : aiEnrichment.getVerified();
    }
}
