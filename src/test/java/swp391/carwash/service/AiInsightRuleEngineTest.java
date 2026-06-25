package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import swp391.carwash.repository.BookingBehaviorMonthlyView;

class AiInsightRuleEngineTest {
    private AiInsightRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new AiInsightRuleEngine();
    }

    @Test
    void vipSegmentWhenCompletedCountReachesThreshold() {
        Map<String, AiInsightRuleEngine.InsightDraft> drafts = byType(ruleEngine.evaluate(
                behavior(5, 5, 0, 0, new BigDecimal("500000"), 1)));

        assertEquals("VIP", drafts.get(AiInsightRuleEngine.CUSTOMER_SEGMENT).label());
        assertEquals(new BigDecimal("0.90"), drafts.get(AiInsightRuleEngine.CUSTOMER_SEGMENT).confidenceScore());
    }

    @Test
    void atRiskSegmentTakesPriorityOverVip() {
        Map<String, AiInsightRuleEngine.InsightDraft> drafts = byType(ruleEngine.evaluate(
                behavior(7, 5, 1, 1, new BigDecimal("1200000"), 1)));

        assertEquals("AT_RISK", drafts.get(AiInsightRuleEngine.CUSTOMER_SEGMENT).label());
        assertEquals("HIGH", drafts.get(AiInsightRuleEngine.CHURN_RISK).label());
    }

    @Test
    void newSegmentWhenCustomerHasOneBooking() {
        Map<String, AiInsightRuleEngine.InsightDraft> drafts = byType(ruleEngine.evaluate(
                behavior(1, 1, 0, 0, new BigDecimal("80000"), null)));

        assertEquals("NEW", drafts.get(AiInsightRuleEngine.CUSTOMER_SEGMENT).label());
        assertEquals("SECOND_VISIT_OFFER", drafts.get(AiInsightRuleEngine.PROMOTION_RECOMMENDATION).label());
    }

    @Test
    void churnRiskMediumWhenThereIsOneCancelledOrNoShowBooking() {
        Map<String, AiInsightRuleEngine.InsightDraft> drafts = byType(ruleEngine.evaluate(
                behavior(3, 2, 1, 0, new BigDecimal("250000"), 2)));

        assertEquals("MEDIUM", drafts.get(AiInsightRuleEngine.CHURN_RISK).label());
        assertEquals(new BigDecimal("0.75"), drafts.get(AiInsightRuleEngine.CHURN_RISK).confidenceScore());
    }

    @Test
    void serviceRecommendationUsesPreferredSlotWhenAvailable() {
        Map<String, AiInsightRuleEngine.InsightDraft> drafts = byType(ruleEngine.evaluate(
                behavior(3, 2, 0, 0, new BigDecimal("250000"), 9)));

        assertEquals("PREFERRED_SLOT_REBOOK", drafts.get(AiInsightRuleEngine.SERVICE_RECOMMENDATION).label());
    }

    private Map<String, AiInsightRuleEngine.InsightDraft> byType(List<AiInsightRuleEngine.InsightDraft> drafts) {
        return drafts.stream().collect(java.util.stream.Collectors.toMap(
                AiInsightRuleEngine.InsightDraft::insightType,
                draft -> draft));
    }

    private BookingBehaviorMonthlyView behavior(
            int totalBookings,
            int completedCount,
            int cancelledCount,
            int noShowCount,
            BigDecimal totalSpent,
            Integer preferredSlotId) {
        return new TestBookingBehaviorMonthlyView(
                10,
                1,
                "2026-06",
                totalBookings,
                completedCount,
                cancelledCount,
                noShowCount,
                totalSpent,
                preferredSlotId,
                "ACTIVE",
                OffsetDateTime.now());
    }

    private record TestBookingBehaviorMonthlyView(
            Integer userId,
            Integer garageId,
            String monthYear,
            Integer totalBookings,
            Integer completedCount,
            Integer cancelledCount,
            Integer noShowCount,
            BigDecimal totalSpent,
            Integer preferredSlotId,
            String status,
            OffsetDateTime lastUpdated
    ) implements BookingBehaviorMonthlyView {
        @Override
        public Integer getUserId() {
            return userId;
        }

        @Override
        public Integer getGarageId() {
            return garageId;
        }

        @Override
        public String getMonthYear() {
            return monthYear;
        }

        @Override
        public Integer getTotalBookings() {
            return totalBookings;
        }

        @Override
        public Integer getCompletedCount() {
            return completedCount;
        }

        @Override
        public Integer getCancelledCount() {
            return cancelledCount;
        }

        @Override
        public Integer getNoShowCount() {
            return noShowCount;
        }

        @Override
        public BigDecimal getTotalSpent() {
            return totalSpent;
        }

        @Override
        public Integer getPreferredSlotId() {
            return preferredSlotId;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public OffsetDateTime getLastUpdated() {
            return lastUpdated;
        }
    }
}
