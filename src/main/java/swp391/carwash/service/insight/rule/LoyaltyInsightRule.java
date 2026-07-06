package swp391.carwash.service.insight.rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class LoyaltyInsightRule implements InsightRule {
    public static final String LOW_POINT_REDEMPTION = "LOW_POINT_REDEMPTION";
    public static final String LOYALTY_UNUSED_POINTS = "LOYALTY_UNUSED_POINTS";
    public static final String LOYALTY_EFFECTIVE = "LOYALTY_EFFECTIVE";

    @Override
    public InsightType type() {
        return InsightType.LOYALTY;
    }

    @Override
    public List<InsightResponse> evaluate(InsightAnalysisContext context) {
        List<InsightResponse> insights = new ArrayList<>();
        InsightMetrics current = context.current();

        if (current.totalPointsEarned() > 0
                && context.active(LOW_POINT_REDEMPTION)
                && current.redemptionRateAgainstEarnedPointsPercent() < context.threshold(LOW_POINT_REDEMPTION, InsightThresholds.LOW_LOYALTY_REDEMPTION_RATE_PERCENT)) {
            insights.add(new InsightResponse(
                    LOW_POINT_REDEMPTION,
                    type(),
                    context.severity(LOW_POINT_REDEMPTION, InsightSeverity.WARNING),
                    "Chính sách đổi điểm có thể chưa đủ hấp dẫn",
                    "Điểm đã dùng chỉ tương đương " + InsightText.percent(current.redemptionRateAgainstEarnedPointsPercent())
                            + " so với điểm đã tích trong kỳ.",
                    "Khách tích " + InsightText.number(current.totalPointsEarned()) + " điểm và dùng "
                            + InsightText.number(current.totalPointsRedeemed()) + " điểm.",
                    "Khách đang tích điểm nhưng chưa có nhiều hành vi đổi điểm, làm giảm tác dụng giữ chân.",
                    "Cân nhắc giảm ngưỡng đổi điểm, thêm ưu đãi cụ thể hoặc nhắc khách về quyền lợi có thể đổi.",
                    "loyaltyRedemptionRate",
                    context.createdAt()));
        }

        if (current.loyaltyAccountsWithAvailablePoints() >= 5
                && context.active(LOYALTY_UNUSED_POINTS)
                && current.redemptionRateAgainstAccountsWithPointsPercent() < context.threshold(LOYALTY_UNUSED_POINTS, InsightThresholds.LOW_REDEMPTION_ACCOUNT_RATE_PERCENT)) {
            insights.add(new InsightResponse(
                    LOYALTY_UNUSED_POINTS,
                    type(),
                    context.severity(LOYALTY_UNUSED_POINTS, InsightSeverity.WARNING),
                    "Nhiều khách có điểm nhưng ít sử dụng",
                    "Chỉ " + InsightText.percent(current.redemptionRateAgainstAccountsWithPointsPercent())
                            + " khách có điểm khả dụng phát sinh đổi điểm trong kỳ.",
                    "Có " + InsightText.number(current.loyaltyAccountsWithAvailablePoints()) + " tài khoản đang còn điểm khả dụng.",
                    "Nếu khách không thấy hoặc không dùng được điểm, chương trình loyalty sẽ kém hiệu quả.",
                    "Hiển thị rõ điểm có thể đổi và gửi thông báo nhắc dùng điểm sau khi khách hoàn tất dịch vụ.",
                    "availablePoints",
                    context.createdAt()));
        }

        Set<Integer> redeemedCustomers = current.redeemedCustomerIds();
        Set<Integer> nonRedeemedCustomers = new HashSet<>(current.customerIds());
        nonRedeemedCustomers.removeAll(redeemedCustomers);
        if (!redeemedCustomers.isEmpty() && !nonRedeemedCustomers.isEmpty()) {
            double redeemedAverage = current.averageOrdersForCustomers(redeemedCustomers);
            double nonRedeemedAverage = current.averageOrdersForCustomers(nonRedeemedCustomers);
            if (nonRedeemedAverage > 0
                    && context.active(LOYALTY_EFFECTIVE)
                    && redeemedAverage > nonRedeemedAverage * (1 + context.threshold(LOYALTY_EFFECTIVE, InsightThresholds.REDEEMER_RETURN_LIFT_PERCENT) / 100)) {
                double lift = (redeemedAverage - nonRedeemedAverage) * 100 / nonRedeemedAverage;
                insights.add(new InsightResponse(
                        LOYALTY_EFFECTIVE,
                        type(),
                        context.severity(LOYALTY_EFFECTIVE, InsightSeverity.POSITIVE),
                        "Chương trình tích điểm có tác động tích cực",
                        "Khách có đổi điểm quay lại nhiều hơn nhóm chưa đổi điểm " + InsightText.percent(lift) + ".",
                        "Nhóm đổi điểm trung bình " + InsightText.number(Math.round(redeemedAverage))
                                + " đơn/khách, nhóm còn lại trung bình " + InsightText.number(Math.round(nonRedeemedAverage)) + " đơn/khách.",
                        "Dữ liệu cho thấy đổi điểm có thể giúp tăng khả năng quay lại.",
                        "Tiếp tục duy trì loyalty và thử chiến dịch nhân đôi điểm vào thời gian thấp điểm.",
                        "loyaltyRetention",
                        context.createdAt()));
            }
        }

        return insights;
    }
}
