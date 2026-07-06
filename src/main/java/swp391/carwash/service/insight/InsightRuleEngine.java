package swp391.carwash.service.insight;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import swp391.carwash.dto.insight.InsightResponse;
import swp391.carwash.enums.InsightType;

@Component
@RequiredArgsConstructor
public class InsightRuleEngine {
    private final List<InsightRule> rules;

    public List<InsightResponse> generate(InsightAnalysisContext context, InsightType typeFilter) {
        return rules.stream()
                .filter(rule -> rule.supports(typeFilter))
                .flatMap(rule -> rule.evaluate(context).stream())
                .sorted(Comparator
                        .comparingInt((InsightResponse insight) -> insight.severity().getPriority())
                        .thenComparing(insight -> insight.type().name())
                        .thenComparing(InsightResponse::id))
                .limit(InsightThresholds.MAX_INSIGHTS)
                .toList();
    }
}
