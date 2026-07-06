package swp391.carwash.dto.insight;

import java.util.List;

public record InsightGenerateResponse(
        InsightPeriod period,
        int generatedCount,
        int createdCount,
        int updatedCount,
        List<BusinessInsightResponse> insights,
        String message
) {
}
