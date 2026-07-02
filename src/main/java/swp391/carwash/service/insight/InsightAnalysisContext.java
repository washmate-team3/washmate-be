package swp391.carwash.service.insight;

import java.time.OffsetDateTime;

public record InsightAnalysisContext(
        InsightMetrics current,
        InsightMetrics previous,
        OffsetDateTime createdAt,
        InsightRuleConfigRegistry ruleConfigs
) {
    public InsightAnalysisContext(InsightMetrics current, InsightMetrics previous, OffsetDateTime createdAt) {
        this(current, previous, createdAt, InsightRuleConfigRegistry.empty());
    }

    public boolean active(String ruleCode) {
        return ruleConfigs.active(ruleCode);
    }

    public double threshold(String ruleCode, double fallback) {
        return ruleConfigs.threshold(ruleCode, fallback);
    }

    public swp391.carwash.enums.InsightSeverity severity(
            String ruleCode,
            swp391.carwash.enums.InsightSeverity fallback) {
        return ruleConfigs.severity(ruleCode, fallback);
    }
}
