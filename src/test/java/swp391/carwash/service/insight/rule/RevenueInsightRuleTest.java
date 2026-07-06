package swp391.carwash.service.insight.rule;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import swp391.carwash.dto.insight.InsightResponse;
import swp391.carwash.entity.InsightRuleConfig;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.enums.ComparisonOperator;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightType;
import swp391.carwash.service.insight.InsightAnalysisContext;
import swp391.carwash.service.insight.InsightMetrics;
import swp391.carwash.service.insight.InsightRuleConfigRegistry;
import swp391.carwash.service.insight.InsightTestData;

class RevenueInsightRuleTest {
    private final RevenueInsightRule rule = new RevenueInsightRule();

    @Test
    void generatesRevenueGrowthInsightWhenRevenueIncreasesMoreThanThreshold() {
        LocalDate currentFrom = LocalDate.of(2026, 7, 1);
        var booking = InsightTestData.booking(1, 1, BookingStatus.COMPLETED, currentFrom);
        InsightMetrics current = InsightTestData.metrics(
                currentFrom,
                currentFrom,
                List.of(booking),
                List.of(InsightTestData.paidInvoice(1, booking, "120000", OffsetDateTime.parse("2026-07-01T10:00:00+07:00"))),
                List.of(),
                List.of(),
                List.of(booking.getService()),
                Set.of(),
                0);

        LocalDate previousFrom = LocalDate.of(2026, 6, 30);
        var previousBooking = InsightTestData.booking(2, 1, BookingStatus.COMPLETED, previousFrom);
        InsightMetrics previous = InsightTestData.metrics(
                previousFrom,
                previousFrom,
                List.of(previousBooking),
                List.of(InsightTestData.paidInvoice(2, previousBooking, "100000", OffsetDateTime.parse("2026-06-30T10:00:00+07:00"))),
                List.of(),
                List.of(),
                List.of(previousBooking.getService()),
                Set.of(),
                0);

        List<InsightResponse> insights = rule.evaluate(InsightTestData.context(current, previous));

        assertTrue(insights.stream().anyMatch(insight -> RevenueInsightRule.REVENUE_GROWTH.equals(insight.id())));
    }

    @Test
    void generatesWeekendOpportunityFromAverageRevenueLift() {
        LocalDate from = LocalDate.of(2026, 7, 3);
        LocalDate to = LocalDate.of(2026, 7, 5);
        var weekdayBooking = InsightTestData.booking(1, 1, BookingStatus.COMPLETED, from);
        var saturdayBooking = InsightTestData.booking(2, 2, BookingStatus.COMPLETED, from.plusDays(1));
        var sundayBooking = InsightTestData.booking(3, 3, BookingStatus.COMPLETED, from.plusDays(2));
        InsightMetrics current = InsightTestData.metrics(
                from,
                to,
                List.of(weekdayBooking, saturdayBooking, sundayBooking),
                List.of(
                        InsightTestData.paidInvoice(1, weekdayBooking, "100000", OffsetDateTime.parse("2026-07-03T10:00:00+07:00")),
                        InsightTestData.paidInvoice(2, saturdayBooking, "200000", OffsetDateTime.parse("2026-07-04T10:00:00+07:00")),
                        InsightTestData.paidInvoice(3, sundayBooking, "200000", OffsetDateTime.parse("2026-07-05T10:00:00+07:00"))),
                List.of(),
                List.of(),
                List.of(weekdayBooking.getService()),
                Set.of(),
                0);
        InsightMetrics previous = InsightTestData.metrics(
                from.minusDays(3),
                from.minusDays(1),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Set.of(),
                0);

        List<InsightResponse> insights = rule.evaluate(InsightTestData.context(current, previous));

        assertTrue(insights.stream().anyMatch(insight -> RevenueInsightRule.WEEKEND_REVENUE_HIGH.equals(insight.id())));
    }

    @Test
    void doesNotGenerateWhenConfiguredThresholdIsNotMet() {
        LocalDate currentFrom = LocalDate.of(2026, 7, 1);
        var booking = InsightTestData.booking(1, 1, BookingStatus.COMPLETED, currentFrom);
        InsightMetrics current = InsightTestData.metrics(
                currentFrom,
                currentFrom,
                List.of(booking),
                List.of(InsightTestData.paidInvoice(1, booking, "120000", OffsetDateTime.parse("2026-07-01T10:00:00+07:00"))),
                List.of(),
                List.of(),
                List.of(booking.getService()),
                Set.of(),
                0);
        var previousBooking = InsightTestData.booking(2, 1, BookingStatus.COMPLETED, currentFrom.minusDays(1));
        InsightMetrics previous = InsightTestData.metrics(
                currentFrom.minusDays(1),
                currentFrom.minusDays(1),
                List.of(previousBooking),
                List.of(InsightTestData.paidInvoice(2, previousBooking, "100000", OffsetDateTime.parse("2026-06-30T10:00:00+07:00"))),
                List.of(),
                List.of(),
                List.of(previousBooking.getService()),
                Set.of(),
                0);
        InsightRuleConfig config = InsightRuleConfig.builder()
                .ruleCode(RevenueInsightRule.REVENUE_GROWTH)
                .ruleName("Revenue growth")
                .type(InsightType.REVENUE)
                .thresholdValue(new BigDecimal("25"))
                .comparisonOperator(ComparisonOperator.GREATER_THAN)
                .severity(InsightSeverity.POSITIVE)
                .active(true)
                .build();
        InsightAnalysisContext context = new InsightAnalysisContext(
                current,
                previous,
                OffsetDateTime.parse("2026-07-31T10:00:00+07:00"),
                InsightRuleConfigRegistry.from(List.of(config)));

        List<InsightResponse> insights = rule.evaluate(context);

        assertTrue(insights.stream().noneMatch(insight -> RevenueInsightRule.REVENUE_GROWTH.equals(insight.id())));
    }

    @Test
    void doesNotGenerateInactiveRule() {
        LocalDate currentFrom = LocalDate.of(2026, 7, 1);
        var booking = InsightTestData.booking(1, 1, BookingStatus.COMPLETED, currentFrom);
        InsightMetrics current = InsightTestData.metrics(
                currentFrom,
                currentFrom,
                List.of(booking),
                List.of(InsightTestData.paidInvoice(1, booking, "120000", OffsetDateTime.parse("2026-07-01T10:00:00+07:00"))),
                List.of(),
                List.of(),
                List.of(booking.getService()),
                Set.of(),
                0);
        var previousBooking = InsightTestData.booking(2, 1, BookingStatus.COMPLETED, currentFrom.minusDays(1));
        InsightMetrics previous = InsightTestData.metrics(
                currentFrom.minusDays(1),
                currentFrom.minusDays(1),
                List.of(previousBooking),
                List.of(InsightTestData.paidInvoice(2, previousBooking, "100000", OffsetDateTime.parse("2026-06-30T10:00:00+07:00"))),
                List.of(),
                List.of(),
                List.of(previousBooking.getService()),
                Set.of(),
                0);
        InsightRuleConfig config = InsightRuleConfig.builder()
                .ruleCode(RevenueInsightRule.REVENUE_GROWTH)
                .ruleName("Revenue growth")
                .type(InsightType.REVENUE)
                .thresholdValue(new BigDecimal("15"))
                .comparisonOperator(ComparisonOperator.GREATER_THAN)
                .severity(InsightSeverity.POSITIVE)
                .active(false)
                .build();
        InsightAnalysisContext context = new InsightAnalysisContext(
                current,
                previous,
                OffsetDateTime.parse("2026-07-31T10:00:00+07:00"),
                InsightRuleConfigRegistry.from(List.of(config)));

        List<InsightResponse> insights = rule.evaluate(context);

        assertTrue(insights.stream().noneMatch(insight -> RevenueInsightRule.REVENUE_GROWTH.equals(insight.id())));
    }
}
