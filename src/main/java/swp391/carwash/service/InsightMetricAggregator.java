package swp391.carwash.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.insight.InsightContext;
import swp391.carwash.dto.insight.MetricBreakdown;
import swp391.carwash.dto.insight.TrendPoint;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.BusinessInsight;
import swp391.carwash.entity.Garage;
import swp391.carwash.entity.Invoice;
import swp391.carwash.enums.InsightSource;
import swp391.carwash.service.insight.InsightAnalysisContext;
import swp391.carwash.service.insight.InsightMetrics;

@Service
@RequiredArgsConstructor
public class InsightMetricAggregator {
    private static final int TREND_POINT_COUNT = 4;

    private final ReportAggregationService reportAggregationService;

    @Transactional(readOnly = true)
    public InsightContext buildContext(BusinessInsight insight) {
        InsightAnalysisContext analysis = reportAggregationService.aggregate(insight.getFromDate(), insight.getToDate());
        InsightMetrics current = analysis.current();
        InsightMetrics previous = analysis.previous();

        Map<String, Object> headline = new LinkedHashMap<>();
        headline.put("ruleCode", insight.getRuleCode());
        headline.put("type", insight.getType() != null ? insight.getType().name() : null);
        headline.put("severity", insight.getSeverity() != null ? insight.getSeverity().name() : null);
        headline.put("title", insight.getTitle());
        headline.put("summary", insight.getSummary());
        headline.put("evidence", insight.getEvidence());
        headline.put("meaning", insight.getMeaning());
        headline.put("recommendation", insight.getRecommendation());
        headline.put("relatedMetric", insight.getRelatedMetric());
        headline.put("current", summaryMetrics(current, previous));
        headline.put("previous", summaryMetrics(previous, null));

        Map<String, Object> scope = new LinkedHashMap<>();
        scope.put("fromDate", insight.getFromDate());
        scope.put("toDate", insight.getToDate());
        scope.put("branchId", "ALL");
        scope.put("source", InsightSource.RULE_BASED.name());
        scope.put("dataPolicy", "Only backend-aggregated numbers are included. No customer PII is provided.");

        return new InsightContext(
                insight.getType() != null ? insight.getType().name() : null,
                headline,
                breakdown(current),
                trend(insight.getFromDate(), insight.getToDate()),
                scope);
    }

    private List<MetricBreakdown> breakdown(InsightMetrics metrics) {
        List<MetricBreakdown> breakdown = new ArrayList<>();

        metrics.serviceStats().stream()
                .sorted(Comparator.comparing(InsightMetrics.ServiceStats::revenue).reversed())
                .forEach(service -> {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("serviceId", service.serviceId());
                    values.put("orderCount", service.orderCount());
                    values.put("orderSharePercent", service.orderSharePercent(metrics.totalOrders()));
                    values.put("revenue", service.revenue());
                    values.put("revenueSharePercent", service.revenueSharePercent(metrics.totalRevenue()));
                    breakdown.add(new MetricBreakdown("SERVICE", service.serviceName(), values));
                });

        metrics.timeSlotStats().stream()
                .sorted(Comparator.comparingLong(InsightMetrics.TimeSlotStats::orderCount).reversed())
                .forEach(slot -> {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("orderCount", slot.orderCount());
                    values.put("orderSharePercent", slot.orderSharePercent(metrics.totalOrders()));
                    breakdown.add(new MetricBreakdown("TIME_SLOT", slot.label(), values));
                });

        breakdown.addAll(garageBreakdown(metrics));
        return List.copyOf(breakdown);
    }

    private List<MetricBreakdown> garageBreakdown(InsightMetrics metrics) {
        Map<Integer, Long> ordersByGarage = metrics.bookings().stream()
                .filter(booking -> booking.getGarage() != null && booking.getGarage().getId() != null)
                .collect(Collectors.groupingBy(
                        booking -> booking.getGarage().getId(),
                        LinkedHashMap::new,
                        Collectors.counting()));
        Map<Integer, BigDecimal> revenueByGarage = metrics.paidInvoices().stream()
                .filter(invoice -> invoice.getGarage() != null && invoice.getGarage().getId() != null)
                .collect(Collectors.toMap(
                        invoice -> invoice.getGarage().getId(),
                        invoice -> invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount(),
                        BigDecimal::add,
                        LinkedHashMap::new));

        Set<Integer> garageIds = new LinkedHashSet<>();
        garageIds.addAll(ordersByGarage.keySet());
        garageIds.addAll(revenueByGarage.keySet());

        return garageIds.stream()
                .map(garageId -> {
                    BigDecimal revenue = revenueByGarage.getOrDefault(garageId, BigDecimal.ZERO);
                    long orders = ordersByGarage.getOrDefault(garageId, 0L);
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("garageId", garageId);
                    values.put("orderCount", orders);
                    values.put("orderSharePercent", percent(orders, metrics.totalOrders()));
                    values.put("revenue", revenue);
                    values.put("revenueSharePercent", percent(revenue, metrics.totalRevenue()));
                    return new MetricBreakdown("GARAGE", garageName(metrics, garageId), values);
                })
                .sorted(Comparator.comparing(
                        breakdown -> (BigDecimal) breakdown.metrics().get("revenue"),
                        Comparator.reverseOrder()))
                .toList();
    }

    private String garageName(InsightMetrics metrics, Integer garageId) {
        return metrics.bookings().stream()
                .map(Booking::getGarage)
                .filter(garage -> garage != null && garageId.equals(garage.getId()))
                .map(Garage::getName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .or(() -> metrics.paidInvoices().stream()
                        .map(Invoice::getGarage)
                        .filter(garage -> garage != null && garageId.equals(garage.getId()))
                        .map(Garage::getName)
                        .filter(name -> name != null && !name.isBlank())
                        .findFirst())
                .orElse("Garage #" + garageId);
    }

    private List<TrendPoint> trend(LocalDate fromDate, LocalDate toDate) {
        long periodDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        List<TrendPoint> points = new ArrayList<>();
        for (int offset = TREND_POINT_COUNT - 1; offset >= 0; offset--) {
            LocalDate pointTo = toDate.minusDays(periodDays * offset);
            LocalDate pointFrom = pointTo.minusDays(periodDays - 1);
            InsightAnalysisContext pointContext = reportAggregationService.aggregate(pointFrom, pointTo);
            points.add(new TrendPoint(
                    pointFrom + "_to_" + pointTo,
                    summaryMetrics(pointContext.current(), pointContext.previous())));
        }
        return List.copyOf(points);
    }

    private Map<String, Object> summaryMetrics(InsightMetrics current, InsightMetrics previous) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("period", current.period());
        metrics.put("totalRevenue", current.totalRevenue());
        metrics.put("totalOrders", current.totalOrders());
        metrics.put("completedOrders", current.completedOrders());
        metrics.put("cancelledOrders", current.cancelledOrders());
        metrics.put("completedOrderRatePercent", current.completedOrderRatePercent());
        metrics.put("cancelledOrderRatePercent", current.cancelledOrderRatePercent());
        metrics.put("averageRevenuePerOrder", current.averageRevenuePerOrder());
        metrics.put("newCustomers", current.newCustomers());
        metrics.put("returningCustomers", current.returningCustomers());
        metrics.put("totalPointsEarned", current.totalPointsEarned());
        metrics.put("totalPointsRedeemed", current.totalPointsRedeemed());
        metrics.put("topServiceByUsage", current.topServiceByUsage().map(InsightMetrics.ServiceStats::serviceName).orElse(null));
        metrics.put("topServiceByRevenue", current.topServiceByRevenue().map(InsightMetrics.ServiceStats::serviceName).orElse(null));
        metrics.put("peakTimeSlot", current.peakTimeSlot().map(InsightMetrics.TimeSlotStats::label).orElse(null));
        metrics.put("lowTimeSlot", current.lowTimeSlot().map(InsightMetrics.TimeSlotStats::label).orElse(null));
        if (previous != null) {
            metrics.put("revenueChangePercent", current.revenueChangePercent(previous));
            metrics.put("orderChangePercent", current.orderChangePercent(previous));
        }
        return metrics;
    }

    private double percent(long part, long whole) {
        if (whole <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(whole, 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}
