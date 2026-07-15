package swp391.carwash.service.insight;

/**
 * Ảnh chụp trạng thái loyalty/tier tại thời điểm phân tích (không phụ thuộc kỳ),
 * dùng cho các rule bám khung đề: điểm sắp hết hạn, khách sắp lên hạng,
 * làn sóng hạ hạng, và phân bố hạng.
 *
 * <p>Các giá trị được tính sẵn tại {@code ReportAggregationService} để giữ
 * {@link InsightMetrics} thuần logic, không truy vấn DB.
 */
public record LoyaltyStateSnapshot(
        long pointsExpiringSoon,
        long accountsWithExpiringPoints,
        long customersNearNextTier,
        long downgradeCount,
        long totalActiveAccounts,
        long lowestTierAccounts,
        long inactiveCustomers
) {
    public static LoyaltyStateSnapshot empty() {
        return new LoyaltyStateSnapshot(0, 0, 0, 0, 0, 0, 0);
    }

    /** Tỷ lệ tài khoản bị hạ hạng trong kỳ so với tổng tài khoản active. */
    public double downgradeRatePercent() {
        if (totalActiveAccounts <= 0) {
            return 0;
        }
        return (double) downgradeCount * 100 / totalActiveAccounts;
    }

    /** Tỷ lệ tài khoản đang kẹt ở hạng thấp nhất. */
    public double lowestTierSharePercent() {
        if (totalActiveAccounts <= 0) {
            return 0;
        }
        return (double) lowestTierAccounts * 100 / totalActiveAccounts;
    }
}
