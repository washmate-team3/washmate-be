package swp391.carwash.service.insight.rule;

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
public class CustomerInsightRule implements InsightRule {
    public static final String HIGH_RETURNING_CUSTOMER_RATE = "HIGH_RETURNING_CUSTOMER_RATE";
    public static final String LOW_RETURNING_CUSTOMER_RATE = "LOW_RETURNING_CUSTOMER_RATE";
    public static final String HIGH_VALUE_CUSTOMER_GROUP = "HIGH_VALUE_CUSTOMER_GROUP";

    @Override
    public InsightType type() {
        return InsightType.CUSTOMER;
    }

    @Override
    public List<InsightResponse> evaluate(InsightAnalysisContext context) {
        List<InsightResponse> insights = new ArrayList<>();
        InsightMetrics current = context.current();

        if (current.totalOrders() > 0
                && context.active(HIGH_RETURNING_CUSTOMER_RATE)
                && current.returningOrderSharePercent() > context.threshold(HIGH_RETURNING_CUSTOMER_RATE, InsightThresholds.RETURNING_ORDER_SHARE_PERCENT)) {
            insights.add(new InsightResponse(
                    HIGH_RETURNING_CUSTOMER_RATE,
                    type(),
                    context.severity(HIGH_RETURNING_CUSTOMER_RATE, InsightSeverity.POSITIVE),
                    "Khách hàng cũ đang đóng góp lớn",
                    "Đơn từ khách quay lại chiếm " + InsightText.percent(current.returningOrderSharePercent()) + " tổng số đơn.",
                    "Có " + InsightText.number(current.returningCustomers()) + " khách quay lại trong kỳ.",
                    "Tệp khách hàng cũ đang giúp doanh thu ổn định và giảm phụ thuộc vào khách mới.",
                    "Tập trung chăm sóc khách thân thiết bằng tích điểm, voucher hoặc ưu đãi sinh nhật.",
                    "returningCustomers",
                    context.createdAt()));
        }

        if (current.customerIds().size() >= 3
                && current.newCustomerSharePercent() > InsightThresholds.NEW_CUSTOMER_SHARE_PERCENT
                && context.active(LOW_RETURNING_CUSTOMER_RATE)
                && current.returningOrderSharePercent() < context.threshold(LOW_RETURNING_CUSTOMER_RATE, InsightThresholds.LOW_RETURNING_ORDER_SHARE_PERCENT)) {
            insights.add(new InsightResponse(
                    LOW_RETURNING_CUSTOMER_RATE,
                    type(),
                    context.severity(LOW_RETURNING_CUSTOMER_RATE, InsightSeverity.WARNING),
                    "Thu hút khách mới tốt nhưng giữ chân còn thấp",
                    "Khách mới chiếm " + InsightText.percent(current.newCustomerSharePercent())
                            + ", nhưng đơn từ khách quay lại chỉ chiếm " + InsightText.percent(current.returningOrderSharePercent()) + ".",
                    "Có " + InsightText.number(current.newCustomers()) + " khách mới và "
                            + InsightText.number(current.returningCustomers()) + " khách quay lại trong kỳ.",
                    "Hệ thống có khả năng thu hút khách mới nhưng chưa tạo đủ lý do để khách quay lại.",
                    "Cải thiện trải nghiệm sau dịch vụ và gửi ưu đãi cho lần quay lại tiếp theo.",
                    "customerRetention",
                    context.createdAt()));
        }

        double topCustomerRevenueShare = current.topCustomerRevenueSharePercent();
        if (context.active(HIGH_VALUE_CUSTOMER_GROUP)
                && topCustomerRevenueShare > context.threshold(HIGH_VALUE_CUSTOMER_GROUP, InsightThresholds.TOP_CUSTOMER_REVENUE_SHARE_PERCENT)) {
            insights.add(new InsightResponse(
                    HIGH_VALUE_CUSTOMER_GROUP,
                    type(),
                    context.severity(HIGH_VALUE_CUSTOMER_GROUP, InsightSeverity.OPPORTUNITY),
                    "Một nhóm khách hàng đang tạo doanh thu cao",
                    "Nhóm 20% khách có doanh thu cao nhất đóng góp " + InsightText.percent(topCustomerRevenueShare) + " tổng doanh thu.",
                    "Phân tích dựa trên " + InsightText.number(current.revenueByCustomer().size()) + " khách có phát sinh doanh thu trong kỳ.",
                    "Doanh thu đang phụ thuộc đáng kể vào nhóm khách hàng giá trị cao.",
                    "Nên có chính sách chăm sóc riêng như ưu đãi định kỳ, nhắc lịch rửa xe hoặc combo cao cấp.",
                    "customerRevenue",
                    context.createdAt()));
        }

        return insights;
    }
}
