package swp391.carwash.dto.insight;

import java.util.Map;

public record MetricBreakdown(
        String dimension,
        String label,
        Map<String, Object> metrics
) {
}
