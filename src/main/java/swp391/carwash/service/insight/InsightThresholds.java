package swp391.carwash.service.insight;

public final class InsightThresholds {
    public static final int MAX_INSIGHTS = 10;
    public static final double REVENUE_CHANGE_PERCENT = 15.0;
    public static final double WEEKEND_REVENUE_LIFT_PERCENT = 30.0;
    public static final double ORDER_CANCEL_WARNING_PERCENT = 10.0;
    public static final double ORDER_CANCEL_CRITICAL_PERCENT = 20.0;
    public static final double ORDER_GROWTH_WITH_LOW_REVENUE_PERCENT = 15.0;
    public static final double PEAK_SLOT_SHARE_PERCENT = 30.0;
    public static final double SERVICE_MAIN_REVENUE_SHARE_PERCENT = 40.0;
    public static final double HIGH_VALUE_SERVICE_REVENUE_SHARE_PERCENT = 25.0;
    public static final double HIGH_VALUE_SERVICE_ORDER_SHARE_PERCENT = 15.0;
    public static final long MIN_ORDERS_FOR_SERVICE_LOW_USAGE = 10;
    public static final double RETURNING_ORDER_SHARE_PERCENT = 50.0;
    public static final double NEW_CUSTOMER_SHARE_PERCENT = 50.0;
    public static final double LOW_RETURNING_ORDER_SHARE_PERCENT = 25.0;
    public static final double TOP_CUSTOMER_REVENUE_SHARE_PERCENT = 50.0;
    public static final int MIN_CUSTOMERS_FOR_CONCENTRATION = 5;
    public static final double LOW_LOYALTY_REDEMPTION_RATE_PERCENT = 20.0;
    public static final double LOW_REDEMPTION_ACCOUNT_RATE_PERCENT = 10.0;
    public static final double REDEEMER_RETURN_LIFT_PERCENT = 20.0;

    // --- Tier/Loyalty health (bám khung đề SU26SWP01) ---
    // Số ngày trước khi điểm hết hạn thì bắt đầu cảnh báo. Căn cứ: đủ 1 chu kỳ rửa (~4 tuần) để khách quay lại đổi điểm.
    public static final int EXPIRING_WINDOW_DAYS = 30;
    // Ngưỡng điểm tối thiểu đang sắp hết hạn để phát cảnh báo (tránh nhiễu khi chỉ vài điểm lẻ).
    public static final double MIN_EXPIRING_POINTS = 100.0;
    // Khoảng cách điểm tối đa tới hạng kế để coi là "sắp lên hạng". Tương đương ~1-2 lần rửa nữa.
    public static final int UPGRADE_GAP_POINTS = 50;
    // Số khách tối thiểu đang sát ngưỡng lên hạng để đáng phát insight.
    public static final double MIN_CUSTOMERS_NEAR_TIER = 3.0;
    // Tỷ lệ tài khoản bị hạ hạng trong kỳ (so tổng tài khoản active) coi là bất thường.
    public static final double DOWNGRADE_RATE_PERCENT = 15.0;
    // Tỷ lệ tài khoản kẹt ở hạng thấp nhất coi là lệch phân bố hạng.
    public static final double LOWEST_TIER_SHARE_PERCENT = 80.0;
    // Số tài khoản tối thiểu để đánh giá phân bố hạng cho có ý nghĩa thống kê.
    public static final long MIN_ACCOUNTS_FOR_TIER_DISTRIBUTION = 5;
    // Số ngày không quay lại thì coi là khách "inactive" (dùng cho tệp win-back).
    public static final int INACTIVE_DAYS = 45;
    // Số khách inactive tối thiểu để phát insight win-back.
    public static final double MIN_INACTIVE_CUSTOMERS = 3.0;

    private InsightThresholds() {
    }
}
