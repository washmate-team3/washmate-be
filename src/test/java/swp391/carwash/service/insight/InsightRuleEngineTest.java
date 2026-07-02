package swp391.carwash.service.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import swp391.carwash.dto.insight.InsightResponse;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightType;

class InsightRuleEngineTest {
    @Test
    void sortsInsightsBySeverityAndHonorsTypeFilter() {
        InsightRule warningRevenueRule = new StaticRule(new InsightResponse(
                "REVENUE_WARNING",
                InsightType.REVENUE,
                InsightSeverity.WARNING,
                "Revenue warning",
                "summary",
                "evidence",
                "meaning",
                "recommendation",
                "revenue",
                OffsetDateTime.parse("2026-07-01T10:00:00+07:00")));
        InsightRule criticalOrderRule = new StaticRule(new InsightResponse(
                "ORDER_CRITICAL",
                InsightType.ORDER,
                InsightSeverity.CRITICAL,
                "Order critical",
                "summary",
                "evidence",
                "meaning",
                "recommendation",
                "orders",
                OffsetDateTime.parse("2026-07-01T10:00:00+07:00")));
        InsightRuleEngine engine = new InsightRuleEngine(List.of(warningRevenueRule, criticalOrderRule));
        InsightAnalysisContext context = InsightTestData.context(emptyMetrics(), emptyMetrics());

        List<InsightResponse> allInsights = engine.generate(context, null);
        List<InsightResponse> explicitAllInsights = engine.generate(context, InsightType.ALL);
        List<InsightResponse> revenueInsights = engine.generate(context, InsightType.REVENUE);

        assertEquals("ORDER_CRITICAL", allInsights.getFirst().id());
        assertEquals(allInsights, explicitAllInsights);
        assertEquals(List.of("REVENUE_WARNING"), revenueInsights.stream().map(InsightResponse::id).toList());
    }

    private InsightMetrics emptyMetrics() {
        LocalDate date = LocalDate.of(2026, 7, 1);
        return InsightTestData.metrics(date, date, List.of(), List.of(), List.of(), List.of(), List.of(), Set.of(), 0);
    }

    private record StaticRule(InsightResponse response) implements InsightRule {
        @Override
        public InsightType type() {
            return response.type();
        }

        @Override
        public List<InsightResponse> evaluate(InsightAnalysisContext context) {
            return List.of(response);
        }
    }
}
