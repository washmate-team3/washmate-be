package swp391.carwash.service.insight.rule;

import java.math.BigDecimal;
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
public class OrderInsightRule implements InsightRule {
    public static final String ORDER_CANCEL_RATE_HIGH = "ORDER_CANCEL_RATE_HIGH";
    public static final String ORDER_VALUE_LOW = "ORDER_VALUE_LOW";
    public static final String PEAK_HOUR_ORDERS = "PEAK_HOUR_ORDERS";

    @Override
    public InsightType type() {
        return InsightType.ORDER;
    }

    @Override
    public List<InsightResponse> evaluate(InsightAnalysisContext context) {
        List<InsightResponse> insights = new ArrayList<>();
        InsightMetrics current = context.current();
        InsightMetrics previous = context.previous();

        if (current.totalOrders() > 0
                && context.active(ORDER_CANCEL_RATE_HIGH)
                && current.cancelledOrderRatePercent() > context.threshold(ORDER_CANCEL_RATE_HIGH, InsightThresholds.ORDER_CANCEL_WARNING_PERCENT)) {
            InsightSeverity severity = current.cancelledOrderRatePercent() > InsightThresholds.ORDER_CANCEL_CRITICAL_PERCENT
                    ? InsightSeverity.CRITICAL
                    : context.severity(ORDER_CANCEL_RATE_HIGH, InsightSeverity.WARNING);
            insights.add(new InsightResponse(
                    ORDER_CANCEL_RATE_HIGH,
                    type(),
                    severity,
                    "Tỷ lệ đơn hủy đang cao",
                    "Tỷ lệ đơn hủy/từ chối/no-show trong kỳ là " + InsightText.percent(current.cancelledOrderRatePercent()) + ".",
                    "Có " + InsightText.number(current.cancelledOrders()) + " đơn không hoàn tất trên tổng "
                            + InsightText.number(current.totalOrders()) + " đơn.",
                    "Tỷ lệ đơn không hoàn tất cao có thể làm mất doanh thu và giảm trải nghiệm khách hàng.",
                    "Kiểm tra thời gian chờ, lịch hẹn, quy trình xác nhận đơn và lý do khách hủy để giảm thất thoát.",
                    "cancelledOrderRate",
                    context.createdAt()));
        }

        if (previous.totalOrders() > 0 && previous.totalRevenue().compareTo(BigDecimal.ZERO) > 0) {
            double orderGrowth = current.orderChangePercent(previous);
            double revenueGrowth = current.revenueChangePercent(previous);
            if (context.active(ORDER_VALUE_LOW)
                    && orderGrowth >= context.threshold(ORDER_VALUE_LOW, InsightThresholds.ORDER_GROWTH_WITH_LOW_REVENUE_PERCENT)
                    && revenueGrowth < orderGrowth - InsightThresholds.REVENUE_CHANGE_PERCENT) {
                insights.add(new InsightResponse(
                        ORDER_VALUE_LOW,
                        type(),
                        context.severity(ORDER_VALUE_LOW, InsightSeverity.OPPORTUNITY),
                        "Giá trị trung bình mỗi đơn đang thấp",
                        "Số đơn tăng " + InsightText.percent(orderGrowth)
                                + " nhưng doanh thu chỉ thay đổi " + InsightText.percent(revenueGrowth) + ".",
                        "Giá trị trung bình hiện tại khoảng " + InsightText.money(current.averageRevenuePerOrder())
                                + " trên mỗi đơn.",
                        "Lượng khách tăng nhưng doanh thu chưa tăng tương ứng, có thể do khách chọn gói giá thấp.",
                        "Gợi ý khách sử dụng combo, nâng cấp dịch vụ hoặc thêm dịch vụ bổ sung phù hợp.",
                        "averageOrderValue",
                        context.createdAt()));
            }
        }

        current.peakTimeSlot().ifPresent(slot -> {
            double slotShare = slot.orderSharePercent(current.totalOrders());
            if (slot.orderCount() >= 2
                    && context.active(PEAK_HOUR_ORDERS)
                    && slotShare > context.threshold(PEAK_HOUR_ORDERS, InsightThresholds.PEAK_SLOT_SHARE_PERCENT)) {
                insights.add(new InsightResponse(
                        PEAK_HOUR_ORDERS,
                        type(),
                        context.severity(PEAK_HOUR_ORDERS, InsightSeverity.OPPORTUNITY),
                        "Có khung giờ cao điểm",
                        "Khung " + slot.label() + " chiếm " + InsightText.percent(slotShare) + " tổng số đơn trong kỳ.",
                        "Khung này có " + InsightText.number(slot.orderCount()) + " đơn trên tổng "
                                + InsightText.number(current.totalOrders()) + " đơn.",
                        "Đơn tập trung vào một khung giờ có thể làm tăng thời gian chờ và áp lực vận hành.",
                        "Bố trí thêm nhân viên hoặc giới hạn nhận lịch trong khung giờ này để giảm ùn tắc.",
                        "peakTimeSlot",
                        context.createdAt()));
            }
        });

        return insights;
    }
}
