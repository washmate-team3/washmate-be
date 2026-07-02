package swp391.carwash.service.insight.rule;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import swp391.carwash.dto.insight.InsightResponse;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.service.insight.InsightMetrics;
import swp391.carwash.service.insight.InsightTestData;

class CustomerInsightRuleTest {
    private final CustomerInsightRule rule = new CustomerInsightRule();

    @Test
    void generatesReturningCustomerInsightWhenReturningOrdersDominate() {
        LocalDate date = LocalDate.of(2026, 7, 1);
        var bookings = List.of(
                InsightTestData.booking(1, 1, BookingStatus.COMPLETED, date),
                InsightTestData.booking(2, 1, BookingStatus.COMPLETED, date),
                InsightTestData.booking(3, 2, BookingStatus.COMPLETED, date),
                InsightTestData.booking(4, 3, BookingStatus.COMPLETED, date));
        InsightMetrics current = InsightTestData.metrics(
                date,
                date,
                bookings,
                List.of(),
                List.of(),
                List.of(),
                List.of(bookings.getFirst().getService()),
                Set.of(1, 2),
                0);
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

        assertTrue(insights.stream().anyMatch(insight -> CustomerInsightRule.HIGH_RETURNING_CUSTOMER_RATE.equals(insight.id())));
    }
}
