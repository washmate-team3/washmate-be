package swp391.carwash.dto.insight;

import java.math.BigDecimal;
import swp391.carwash.entity.InsightRuleConfig;
import swp391.carwash.enums.ComparisonOperator;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightType;

public record InsightRuleConfigResponse(
        Integer id,
        String ruleCode,
        String ruleName,
        InsightType type,
        BigDecimal thresholdValue,
        ComparisonOperator comparisonOperator,
        InsightSeverity severity,
        Boolean active,
        String description
) {
    public static InsightRuleConfigResponse from(InsightRuleConfig config) {
        return new InsightRuleConfigResponse(
                config.getId(),
                config.getRuleCode(),
                config.getRuleName(),
                config.getType(),
                config.getThresholdValue(),
                config.getComparisonOperator(),
                config.getSeverity(),
                config.getActive(),
                config.getDescription());
    }
}
