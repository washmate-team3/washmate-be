package swp391.carwash.dto;

import java.math.BigDecimal;

public record AnalyticsSummaryResponse(
        long totalUsers,
        long activeUsers,
        long totalBookings,
        long pendingBookings,
        long completedBookings,
        long rejectedBookings,
        long totalInvoices,
        long paidInvoices,
        BigDecimal paidRevenue,
        long pendingPayments,
        long paidPayments
) {
}
