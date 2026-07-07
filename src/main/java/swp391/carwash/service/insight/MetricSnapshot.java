package swp391.carwash.service.insight;

import java.math.BigDecimal;
import java.util.Map;
import swp391.carwash.dto.insight.InsightPeriod;

public record MetricSnapshot(
        InsightPeriod period,
        Integer garageId,
        Map<String, BigDecimal> metrics,
        Map<String, Object> details
) {
    public BigDecimal valueOf(String metric) {
        return metric == null ? null : metrics.get(metric);
    }
}
