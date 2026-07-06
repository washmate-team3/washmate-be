package swp391.carwash.dto.insight;

import java.math.BigDecimal;
import swp391.carwash.enums.ComparisonOperator;
import swp391.carwash.enums.InsightSeverity;

public record InsightRuleConfigUpdateRequest(
        BigDecimal thresholdValue,
        ComparisonOperator comparisonOperator,
        InsightSeverity severity,
        Boolean active,
        String description
) {
}
