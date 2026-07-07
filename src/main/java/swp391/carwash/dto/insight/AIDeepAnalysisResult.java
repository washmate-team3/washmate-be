package swp391.carwash.dto.insight;

import java.util.List;

public record AIDeepAnalysisResult(
        List<AIDetectedInsight> insights
) {
}
