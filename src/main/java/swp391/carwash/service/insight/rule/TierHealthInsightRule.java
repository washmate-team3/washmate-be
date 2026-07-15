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

/**
 * Rule giám sát "sức khỏe" của Loyalty Engine & hệ phân hạng — bám khung đề SU26SWP01.
 *
 * <p>Mỗi rule truy ngược được về một dòng chức năng trong đề:
 * <ul>
 *   <li>{@code POINTS_EXPIRING_SOON}  ← "Points expire after 12 months"</li>
 *   <li>{@code UPGRADE_STALL}         ← "Tiered benefits / auto-upgrade"</li>
 *   <li>{@code DOWNGRADE_WAVE}        ← "auto-downgrade (monthly review)"</li>
 *   <li>{@code TIER_DISTRIBUTION_SKEW}← "Tiered benefits (Silver/Gold/Platinum)"</li>
 * </ul>
 */
@Component
public class TierHealthInsightRule implements InsightRule {
    public static final String POINTS_EXPIRING_SOON = "POINTS_EXPIRING_SOON";
    public static final String UPGRADE_STALL = "UPGRADE_STALL";
    public static final String DOWNGRADE_WAVE = "DOWNGRADE_WAVE";
    public static final String TIER_DISTRIBUTION_SKEW = "TIER_DISTRIBUTION_SKEW";

    @Override
    public InsightType type() {
        return InsightType.LOYALTY;
    }

    @Override
    public List<InsightResponse> evaluate(InsightAnalysisContext context) {
        List<InsightResponse> insights = new ArrayList<>();
        InsightMetrics current = context.current();

        // 1. Điểm sắp hết hạn → nhắc khách đổi (gắn action gửi email + voucher)
        if (context.active(POINTS_EXPIRING_SOON)
                && current.pointsExpiringSoon()
                > context.threshold(POINTS_EXPIRING_SOON, InsightThresholds.MIN_EXPIRING_POINTS)) {
            insights.add(new InsightResponse(
                    POINTS_EXPIRING_SOON,
                    type(),
                    context.severity(POINTS_EXPIRING_SOON, InsightSeverity.WARNING),
                    "Nhiều điểm của khách sắp hết hạn",
                    "Có " + InsightText.number(current.pointsExpiringSoon()) + " điểm sẽ hết hạn trong "
                            + InsightThresholds.EXPIRING_WINDOW_DAYS + " ngày tới.",
                    "Có " + InsightText.number(current.accountsWithExpiringPoints())
                            + " tài khoản đang có điểm sắp hết hạn.",
                    "Điểm hết hạn mà không được đổi sẽ làm giảm giá trị chương trình loyalty và bỏ lỡ cơ hội kéo khách quay lại.",
                    "Gửi email nhắc khách đổi điểm kèm ưu đãi trước khi điểm hết hạn.",
                    "pointsExpiringSoon",
                    context.createdAt()));
        }

        // 2. Khách sắp đủ điểm lên hạng → nudge để thúc đẩy lần rửa tiếp theo
        if (context.active(UPGRADE_STALL)
                && current.customersNearNextTier()
                >= context.threshold(UPGRADE_STALL, InsightThresholds.MIN_CUSTOMERS_NEAR_TIER)) {
            insights.add(new InsightResponse(
                    UPGRADE_STALL,
                    type(),
                    context.severity(UPGRADE_STALL, InsightSeverity.OPPORTUNITY),
                    "Nhiều khách sắp đủ điểm lên hạng",
                    "Có " + InsightText.number(current.customersNearNextTier())
                            + " khách chỉ còn thiếu một ít điểm là lên hạng cao hơn.",
                    "Các khách này nằm trong khoảng " + InsightThresholds.UPGRADE_GAP_POINTS
                            + " điểm so với ngưỡng hạng kế tiếp.",
                    "Đây là nhóm dễ kích hoạt: chỉ một lần rửa nữa là thăng hạng, tăng gắn kết với chương trình.",
                    "Gửi nhắc \"còn ít điểm nữa lên hạng\" kèm ưu đãi để thúc đẩy lần rửa tiếp theo.",
                    "customersNearNextTier",
                    context.createdAt()));
        }

        // 3. Làn sóng hạ hạng → xem lại điểm duy trì hạng
        if (current.totalActiveLoyaltyAccounts() > 0
                && context.active(DOWNGRADE_WAVE)
                && current.tierDowngradeRatePercent()
                > context.threshold(DOWNGRADE_WAVE, InsightThresholds.DOWNGRADE_RATE_PERCENT)) {
            insights.add(new InsightResponse(
                    DOWNGRADE_WAVE,
                    type(),
                    context.severity(DOWNGRADE_WAVE, InsightSeverity.WARNING),
                    "Tỷ lệ khách bị hạ hạng đang cao",
                    "Trong kỳ có " + InsightText.number(current.tierDowngradeCount()) + " lượt hạ hạng, chiếm "
                            + InsightText.percent(current.tierDowngradeRatePercent()) + " tổng tài khoản.",
                    "Tổng số tài khoản đang hoạt động là "
                            + InsightText.number(current.totalActiveLoyaltyAccounts()) + ".",
                    "Hạ hạng hàng loạt có thể do điểm duy trì hạng quá cao, khiến khách mất quyền lợi và giảm động lực.",
                    "Xem lại điểm duy trì hạng (maintain points) hoặc chu kỳ đánh giá; cân nhắc ưu đãi giữ hạng.",
                    "tierDowngrade",
                    context.createdAt()));
        }

        // 4. Phân bố hạng lệch → phần lớn kẹt hạng thấp nhất
        if (current.totalActiveLoyaltyAccounts() >= InsightThresholds.MIN_ACCOUNTS_FOR_TIER_DISTRIBUTION
                && context.active(TIER_DISTRIBUTION_SKEW)
                && current.lowestTierSharePercent()
                > context.threshold(TIER_DISTRIBUTION_SKEW, InsightThresholds.LOWEST_TIER_SHARE_PERCENT)) {
            insights.add(new InsightResponse(
                    TIER_DISTRIBUTION_SKEW,
                    type(),
                    context.severity(TIER_DISTRIBUTION_SKEW, InsightSeverity.WARNING),
                    "Phần lớn khách kẹt ở hạng thấp nhất",
                    InsightText.percent(current.lowestTierSharePercent())
                            + " khách đang ở hạng thấp nhất, rất ít khách thăng hạng.",
                    "Có " + InsightText.number(current.lowestTierAccounts()) + " trên "
                            + InsightText.number(current.totalActiveLoyaltyAccounts()) + " tài khoản ở hạng thấp nhất.",
                    "Hệ thống phân hạng gần như không phát huy tác dụng nếu hầu như không ai lên được hạng cao hơn.",
                    "Xem lại ngưỡng điểm lên hạng hoặc tốc độ tích điểm để khách có thể thăng hạng.",
                    "tierDistribution",
                    context.createdAt()));
        }

        return insights;
    }
}
