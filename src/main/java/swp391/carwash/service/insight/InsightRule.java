package swp391.carwash.service.insight;

import java.util.List;
import swp391.carwash.dto.insight.InsightResponse;
import swp391.carwash.enums.InsightType;

public interface InsightRule {
    InsightType type();

    List<InsightResponse> evaluate(InsightAnalysisContext context);

    default boolean supports(InsightType filter) {
        return filter == null || filter == InsightType.ALL || type() == filter;
    }
}
