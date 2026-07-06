package swp391.carwash.service.insight.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import swp391.carwash.dto.insight.InsightResponse;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightType;
import swp391.carwash.service.insight.InsightAnalysisContext;
import swp391.carwash.service.insight.InsightMetrics;
import swp391.carwash.service.insight.InsightRule;
import swp391.carwash.service.insight.InsightText;
import swp391.carwash.service.insight.InsightThresholds;

@Component
public class RevenueInsightRule implements InsightRule {
    public static final String REVENUE_GROWTH = "REVENUE_GROWTH";
    public static final String REVENUE_DROP = "REVENUE_DROP";
    public static final String WEEKEND_REVENUE_HIGH = "WEEKEND_REVENUE_HIGH";

    @Override
    public InsightType type() {
        return InsightType.REVENUE;
    }

    @Override
    public List<InsightResponse> evaluate(InsightAnalysisContext context) {
        List<InsightResponse> insights = new ArrayList<>();
        InsightMetrics current = context.current();
        InsightMetrics previous = context.previous();

        if (previous.totalRevenue().compareTo(BigDecimal.ZERO) > 0) {
            double revenueChange = current.revenueChangePercent(previous);
            if (context.active(REVENUE_GROWTH)
                    && revenueChange >= context.threshold(REVENUE_GROWTH, InsightThresholds.REVENUE_CHANGE_PERCENT)) {
                insights.add(new InsightResponse(
                        REVENUE_GROWTH,
                        type(),
                        context.severity(REVENUE_GROWTH, InsightSeverity.POSITIVE),
                        "Doanh thu đang tăng trưởng tốt",
                        "Doanh thu kỳ hiện tại tăng " + InsightText.percent(revenueChange) + " so với kỳ trước.",
                        "Kỳ hiện tại đạt " + InsightText.money(current.totalRevenue())
                                + ", kỳ trước đạt " + InsightText.money(previous.totalRevenue()) + ".",
                        "Các dịch vụ hoặc chương trình hiện tại đang tạo tín hiệu doanh thu tích cực.",
                        "Tiếp tục duy trì các dịch vụ đang hiệu quả và theo dõi nhóm dịch vụ đóng góp doanh thu chính.",
                        "revenue",
                        context.createdAt()));
            } else if (context.active(REVENUE_DROP)
                    && revenueChange <= -context.threshold(REVENUE_DROP, InsightThresholds.REVENUE_CHANGE_PERCENT)) {
                insights.add(new InsightResponse(
                        REVENUE_DROP,
                        type(),
                        context.severity(REVENUE_DROP, InsightSeverity.WARNING),
                        "Doanh thu đang có dấu hiệu giảm",
                        "Doanh thu kỳ hiện tại giảm " + InsightText.percent(Math.abs(revenueChange)) + " so với kỳ trước.",
                        "Kỳ hiện tại đạt " + InsightText.money(current.totalRevenue())
                                + ", kỳ trước đạt " + InsightText.money(previous.totalRevenue()) + ".",
                        "Doanh thu giảm có thể đến từ số đơn ít hơn, giá trị đơn thấp hơn hoặc khách quay lại giảm.",
                        "Kiểm tra số lượng đơn, dịch vụ bán chạy, tỷ lệ khách quay lại và các chương trình khuyến mãi trong kỳ.",
                        "revenue",
                        context.createdAt()));
            }
        }

        if (current.weekendDayCount() > 0
                && current.weekdayDayCount() > 0
                && current.averageWeekdayRevenuePerDay().compareTo(BigDecimal.ZERO) > 0) {
            double weekendLift = current.averageWeekendRevenuePerDay()
                    .subtract(current.averageWeekdayRevenuePerDay())
                    .multiply(new BigDecimal("100"))
                    .divide(current.averageWeekdayRevenuePerDay(), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            if (context.active(WEEKEND_REVENUE_HIGH)
                    && weekendLift > context.threshold(WEEKEND_REVENUE_HIGH, InsightThresholds.WEEKEND_REVENUE_LIFT_PERCENT)) {
                insights.add(new InsightResponse(
                        WEEKEND_REVENUE_HIGH,
                        type(),
                        context.severity(WEEKEND_REVENUE_HIGH, InsightSeverity.OPPORTUNITY),
                        "Doanh thu cuối tuần cao hơn ngày thường",
                        "Doanh thu trung bình cuối tuần cao hơn ngày thường " + InsightText.percent(weekendLift) + ".",
                        "Trung bình cuối tuần đạt " + InsightText.money(current.averageWeekendRevenuePerDay())
                                + "/ngày, ngày thường đạt " + InsightText.money(current.averageWeekdayRevenuePerDay()) + "/ngày.",
                        "Khách hàng có xu hướng sử dụng dịch vụ nhiều hơn vào cuối tuần.",
                        "Nên tạo chiến dịch nhân đôi điểm hoặc ưu đãi nhẹ từ thứ 2 đến thứ 5 để kéo khách vào thời điểm thấp điểm.",
                        "revenue",
                        context.createdAt()));
            }
        }

        return insights;
    }
}
