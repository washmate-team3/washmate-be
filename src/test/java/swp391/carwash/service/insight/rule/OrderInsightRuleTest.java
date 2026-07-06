package swp391.carwash.service.insight.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import swp391.carwash.dto.insight.InsightResponse;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.service.insight.InsightMetrics;
import swp391.carwash.service.insight.InsightTestData;

class OrderInsightRuleTest {
    private final OrderInsightRule rule = new OrderInsightRule();

    @Test
    void generatesCriticalCancellationInsightWhenFailedOrdersAreHigh() {
        LocalDate date = LocalDate.of(2026, 7, 1);
        List<swp391.carwash.entity.Booking> bookings = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            bookings.add(InsightTestData.booking(i, i, BookingStatus.COMPLETED, date));
        }
        bookings.add(InsightTestData.booking(8, 8, BookingStatus.CANCELLED, date));
        bookings.add(InsightTestData.booking(9, 9, BookingStatus.REJECTED, date));
        bookings.add(InsightTestData.booking(10, 10, BookingStatus.NO_SHOW, date));

        InsightMetrics current = InsightTestData.metrics(
                date,
                date,
                bookings,
                List.of(),
                List.of(),
                List.of(),
                List.of(bookings.getFirst().getService()),
                Set.of(),
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

        assertTrue(insights.stream().anyMatch(insight -> OrderInsightRule.ORDER_CANCEL_RATE_HIGH.equals(insight.id())));
        assertEquals(InsightSeverity.CRITICAL, insights.getFirst().severity());
    }
}
