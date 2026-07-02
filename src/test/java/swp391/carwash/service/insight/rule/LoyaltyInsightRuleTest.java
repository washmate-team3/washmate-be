package swp391.carwash.service.insight.rule;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import swp391.carwash.dto.insight.InsightResponse;
import swp391.carwash.service.insight.InsightMetrics;
import swp391.carwash.service.insight.InsightTestData;

class LoyaltyInsightRuleTest {
    private final LoyaltyInsightRule rule = new LoyaltyInsightRule();

    @Test
    void generatesLowRedemptionInsightWhenEarnedPointsAreNotUsed() {
        LocalDate date = LocalDate.of(2026, 7, 1);
        InsightMetrics current = InsightTestData.metrics(
                date,
                date,
                List.of(),
                List.of(),
                List.of(InsightTestData.earnTransaction(1, 1, 1000, OffsetDateTime.parse("2026-07-01T10:00:00+07:00"))),
                List.of(),
                List.of(),
                Set.of(),
                10);
        InsightMetrics previous = InsightTestData.metrics(
                date.minusDays(1),
                date.minusDays(1),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Set.of(),
                0);

        List<InsightResponse> insights = rule.evaluate(InsightTestData.context(current, previous));

        assertTrue(insights.stream().anyMatch(insight -> LoyaltyInsightRule.LOW_POINT_REDEMPTION.equals(insight.id())));
    }
}
