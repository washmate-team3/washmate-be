package swp391.carwash.dto.insight;

import java.math.BigDecimal;

public record InsightSummary(
        BigDecimal totalRevenue,
        long totalOrders,
        long completedOrders,
        long cancelledOrders,
        long newCustomers,
        long returningCustomers,
        long totalPointsEarned,
        long totalPointsRedeemed,
        BigDecimal completedOrderRate,
        BigDecimal cancelledOrderRate,
        String topServiceByUsage,
        String topServiceByRevenue,
        String peakTimeSlot,
        String lowTimeSlot
) {
}
