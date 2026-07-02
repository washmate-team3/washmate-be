package swp391.carwash.dto.insight;

import java.time.OffsetDateTime;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightType;

public record InsightResponse(
        String id,
        InsightType type,
        InsightSeverity severity,
        String title,
        String summary,
        String evidence,
        String meaning,
        String recommendation,
        String relatedMetric,
        OffsetDateTime createdAt
) {
}
