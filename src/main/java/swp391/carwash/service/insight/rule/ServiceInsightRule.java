package swp391.carwash.service.insight.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
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
public class ServiceInsightRule implements InsightRule {
    public static final String DOMINANT_SERVICE_REVENUE = "DOMINANT_SERVICE_REVENUE";
    public static final String LOW_SERVICE_USAGE = "LOW_SERVICE_USAGE";
    public static final String HIGH_VALUE_SERVICE = "HIGH_VALUE_SERVICE";

    @Override
    public InsightType type() {
        return InsightType.SERVICE;
    }

    @Override
    public List<InsightResponse> evaluate(InsightAnalysisContext context) {
        List<InsightResponse> insights = new ArrayList<>();
        InsightMetrics current = context.current();
        BigDecimal totalRevenue = current.totalRevenue();

        current.topServiceByRevenue().ifPresent(service -> {
            double revenueShare = service.revenueSharePercent(totalRevenue);
            if (context.active(DOMINANT_SERVICE_REVENUE)
                    && revenueShare > context.threshold(DOMINANT_SERVICE_REVENUE, InsightThresholds.SERVICE_MAIN_REVENUE_SHARE_PERCENT)) {
                insights.add(new InsightResponse(
                        DOMINANT_SERVICE_REVENUE,
                        type(),
                        context.severity(DOMINANT_SERVICE_REVENUE, InsightSeverity.OPPORTUNITY),
                        "Một dịch vụ đang là nguồn doanh thu chính",
                        "Dịch vụ " + service.serviceName() + " chiếm " + InsightText.percent(revenueShare) + " tổng doanh thu.",
                        "Dịch vụ này tạo " + InsightText.money(service.revenue()) + " trong kỳ.",
                        "Đây là dịch vụ đang đóng vai trò quan trọng trong kết quả kinh doanh.",
                        "Có thể đẩy mạnh quảng bá dịch vụ này hoặc tạo combo liên quan để tăng giá trị đơn.",
                        "serviceRevenueShare",
                        context.createdAt()));
            }
        });

        current.lowUsageService().ifPresent(service -> {
            double usageShare = service.orderSharePercent(current.totalOrders());
            if (current.totalOrders() >= InsightThresholds.MIN_ORDERS_FOR_SERVICE_LOW_USAGE
                    && context.active(LOW_SERVICE_USAGE)
                    && usageShare < context.threshold(LOW_SERVICE_USAGE, 5.0)) {
                insights.add(new InsightResponse(
                        LOW_SERVICE_USAGE,
                        type(),
                        context.severity(LOW_SERVICE_USAGE, InsightSeverity.WARNING),
                        "Một dịch vụ chưa thu hút khách hàng",
                        "Dịch vụ " + service.serviceName() + " chỉ có " + InsightText.number(service.orderCount()) + " lượt dùng trong kỳ.",
                        "Tổng số đơn trong kỳ là " + InsightText.number(current.totalOrders()) + " đơn.",
                        "Dịch vụ ít được dùng có thể đang gặp vấn đề về giá, mô tả hoặc cách tư vấn.",
                        "Cân nhắc điều chỉnh mô tả, giá hoặc tạo ưu đãi thử dịch vụ để kiểm chứng nhu cầu.",
                        "serviceUsage",
                        context.createdAt()));
            }
        });

        current.serviceStats().stream()
                .filter(service -> service.orderCount() > 0)
                .filter(service -> context.active(HIGH_VALUE_SERVICE))
                .filter(service -> service.orderSharePercent(current.totalOrders()) < InsightThresholds.HIGH_VALUE_SERVICE_ORDER_SHARE_PERCENT)
                .filter(service -> service.revenueSharePercent(totalRevenue) > context.threshold(HIGH_VALUE_SERVICE, InsightThresholds.HIGH_VALUE_SERVICE_REVENUE_SHARE_PERCENT))
                .max(Comparator.comparing(service -> service.revenueSharePercent(totalRevenue)))
                .ifPresent(service -> insights.add(new InsightResponse(
                        HIGH_VALUE_SERVICE,
                        type(),
                        context.severity(HIGH_VALUE_SERVICE, InsightSeverity.OPPORTUNITY),
                        "Dịch vụ có giá trị cao cần được tư vấn thêm",
                        "Dịch vụ " + service.serviceName() + " có ít đơn nhưng đóng góp "
                                + InsightText.percent(service.revenueSharePercent(totalRevenue)) + " doanh thu.",
                        "Dịch vụ này có " + InsightText.number(service.orderCount()) + " lượt dùng và tạo "
                                + InsightText.money(service.revenue()) + ".",
                        "Đây có thể là gói dịch vụ giá trị cao, phù hợp để tăng doanh thu trên mỗi khách.",
                        "Nên đào tạo nhân viên tư vấn dịch vụ này cho nhóm khách phù hợp hoặc đưa vào combo cao cấp.",
                        "serviceRevenue",
                        context.createdAt())));

        return insights;
    }
}
