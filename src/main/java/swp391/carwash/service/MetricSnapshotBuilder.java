package swp391.carwash.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.service.insight.InsightAnalysisContext;
import swp391.carwash.service.insight.InsightMetrics;
import swp391.carwash.service.insight.MetricSnapshot;

@Service
@RequiredArgsConstructor
public class MetricSnapshotBuilder {
    private final ReportAggregationService reportAggregationService;

    @Transactional(readOnly = true)
    public MetricSnapshot build(LocalDate fromDate, LocalDate toDate, Integer garageId) {
        InsightAnalysisContext context = reportAggregationService.aggregate(fromDate, toDate, garageId);
        InsightMetrics current = context.current();
        InsightMetrics previous = context.previous();

        Map<String, BigDecimal> metrics = new LinkedHashMap<>();
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();

        put(metrics, labels, "total_revenue", current.totalRevenue(), "Total paid invoice revenue");
        put(metrics, labels, "total_orders", current.totalOrders(), "Total bookings");
        put(metrics, labels, "completed_orders", current.completedOrders(), "Completed bookings");
        put(metrics, labels, "cancelled_orders", current.cancelledOrders(), "Cancelled/rejected/no-show bookings");
        put(metrics, labels, "completed_order_rate_percent", current.completedOrderRatePercent(), "Completed order rate percent");
        put(metrics, labels, "cancelled_order_rate_percent", current.cancelledOrderRatePercent(), "Cancelled order rate percent");
        put(metrics, labels, "average_revenue_per_order", current.averageRevenuePerOrder(), "Average revenue per order");
        put(metrics, labels, "new_customers", current.newCustomers(), "New customers in period");
        put(metrics, labels, "returning_customers", current.returningCustomers(), "Returning customers in period");
        put(metrics, labels, "returning_order_share_percent", current.returningOrderSharePercent(), "Returning-customer order share percent");
        put(metrics, labels, "total_points_earned", current.totalPointsEarned(), "Loyalty points earned");
        put(metrics, labels, "total_points_redeemed", current.totalPointsRedeemed(), "Loyalty points redeemed");
        put(metrics, labels, "redemption_rate_against_earned_points_percent",
                current.redemptionRateAgainstEarnedPointsPercent(),
                "Redeemed points divided by earned points");
        put(metrics, labels, "revenue_change_percent", current.revenueChangePercent(previous), "Revenue change versus previous equal period");
        put(metrics, labels, "order_change_percent", current.orderChangePercent(previous), "Order change versus previous equal period");

        current.serviceStats().stream()
                .sorted(Comparator.comparing(InsightMetrics.ServiceStats::revenue).reversed())
                .forEach(service -> {
                    String prefix = "service_" + service.serviceId();
                    put(metrics, labels, prefix + "_revenue", service.revenue(), "Revenue for service: " + service.serviceName());
                    put(metrics, labels, prefix + "_revenue_share_percent",
                            service.revenueSharePercent(current.totalRevenue()),
                            "Revenue share percent for service: " + service.serviceName());
                    put(metrics, labels, prefix + "_order_count", service.orderCount(), "Order count for service: " + service.serviceName());
                    put(metrics, labels, prefix + "_order_share_percent",
                            service.orderSharePercent(current.totalOrders()),
                            "Order share percent for service: " + service.serviceName());
                });

        current.timeSlotStats().stream()
                .sorted(Comparator.comparingLong(InsightMetrics.TimeSlotStats::orderCount).reversed())
                .forEach(slot -> {
                    String prefix = "time_slot_" + slug(slot.label());
                    put(metrics, labels, prefix + "_order_count", slot.orderCount(), "Order count for time slot: " + slot.label());
                    put(metrics, labels, prefix + "_order_share_percent",
                            slot.orderSharePercent(current.totalOrders()),
                            "Order share percent for time slot: " + slot.label());
                });

        details.put("metricLabels", labels);
        details.put("topServiceByRevenue", current.topServiceByRevenue().map(InsightMetrics.ServiceStats::serviceName).orElse(null));
        details.put("topServiceByUsage", current.topServiceByUsage().map(InsightMetrics.ServiceStats::serviceName).orElse(null));
        details.put("peakTimeSlot", current.peakTimeSlot().map(InsightMetrics.TimeSlotStats::label).orElse(null));
        details.put("lowTimeSlot", current.lowTimeSlot().map(InsightMetrics.TimeSlotStats::label).orElse(null));
        details.put("scope", Map.of(
                "garageId", garageId == null ? "ALL" : garageId,
                "source", "backend_aggregate",
                "containsPii", false));

        return new MetricSnapshot(current.period(), garageId, metrics, details);
    }

    private void put(Map<String, BigDecimal> metrics, Map<String, String> labels, String key, long value, String label) {
        put(metrics, labels, key, BigDecimal.valueOf(value), label);
    }

    private void put(Map<String, BigDecimal> metrics, Map<String, String> labels, String key, double value, String label) {
        put(metrics, labels, key, BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP), label);
    }

    private void put(Map<String, BigDecimal> metrics, Map<String, String> labels, String key, BigDecimal value, String label) {
        metrics.put(key, value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP));
        labels.put(key, label);
    }

    private String slug(String value) {
        return value == null ? "unknown" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
