package swp391.carwash.dto.insight;

import java.util.List;
import swp391.carwash.enums.InsightAnalysisStatus;

public record AutoWashInsightsResponse(
        InsightPeriod period,
        InsightSummary summary,
        List<BusinessInsightResponse> insights,
        InsightAnalysisStatus analysisStatus,
        String message
) {
}
