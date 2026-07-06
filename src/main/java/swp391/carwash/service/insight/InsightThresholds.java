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

    private InsightThresholds() {
    }
}
