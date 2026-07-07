package swp391.carwash.dto.insight;

import java.util.List;

public record AIDeepAnalysisResponse(
        InsightPeriod period,
        Integer garageId,
        Integer analysisRunId,
        int candidateCount,
        int verifiedCount,
        int rejectedCount,
        List<BusinessInsightResponse> insights,
        List<AIDeepAnalysisRejection> rejectedInsights,
        String message
) {
}
