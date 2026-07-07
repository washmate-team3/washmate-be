package swp391.carwash.dto.insight;

import java.util.List;
import java.util.Map;

public record InsightContext(
        String insightType,
        Map<String, Object> headline,
        List<MetricBreakdown> breakdown,
        List<TrendPoint> trend,
        Map<String, Object> scope
) {
}
