package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import swp391.carwash.dto.insight.AIDetectedInsight;
import swp391.carwash.dto.insight.AiInsightEvidence;
import swp391.carwash.dto.insight.InsightPeriod;
import swp391.carwash.service.insight.MetricSnapshot;

class AiInsightVerifierTest {
    private final AiInsightVerifier verifier = new AiInsightVerifier();

    @Test
    void verifyRejectsAiEvidenceValueThatDoesNotMatchSnapshot() {
        MetricSnapshot snapshot = new MetricSnapshot(
                new InsightPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)),
                null,
                Map.of("revenue_change_percent", new BigDecimal("-23.00")),
                Map.of());
        AIDetectedInsight insight = new AIDetectedInsight(
                "REVENUE_DROP",
                "WARNING",
                "Revenue dropped",
                new AiInsightEvidence("revenue_change_percent", new BigDecimal("-30.00"), "2026-07"),
                "Review pricing");

        var result = verifier.verify(insight, snapshot);

        assertFalse(result.accepted());
    }

    @Test
    void verifyAcceptsAiEvidenceValueThatMatchesSnapshot() {
        MetricSnapshot snapshot = new MetricSnapshot(
                new InsightPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)),
                null,
                Map.of("revenue_change_percent", new BigDecimal("-23.00")),
                Map.of());
        AIDetectedInsight insight = new AIDetectedInsight(
                "REVENUE_DROP",
                "WARNING",
                "Revenue dropped",
                new AiInsightEvidence("revenue_change_percent", new BigDecimal("-23.00"), "2026-07"),
                "Review pricing");

        var result = verifier.verify(insight, snapshot);

        assertTrue(result.accepted());
    }

    @Test
    void verifyRejectsWhenMetricDoesNotExistInSnapshot() {
        MetricSnapshot snapshot = new MetricSnapshot(
                new InsightPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)),
                null,
                Map.of("revenue_change_percent", new BigDecimal("-23.00")),
                Map.of());
        AIDetectedInsight insight = new AIDetectedInsight(
                "REVENUE_DROP",
                "WARNING",
                "Revenue dropped",
                new AiInsightEvidence("fake_metric", new BigDecimal("1.00"), "2026-07"),
                "Review pricing");

        var result = verifier.verify(insight, snapshot);

        assertFalse(result.accepted());
        assertEquals("metric does not exist in snapshot", result.reason());
    }

    @Test
    void verifyRejectsWhenEvidenceIsMissing() {
        MetricSnapshot snapshot = new MetricSnapshot(
                new InsightPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)),
                null,
                Map.of("revenue_change_percent", new BigDecimal("-23.00")),
                Map.of());
        AIDetectedInsight insight = new AIDetectedInsight(
                "REVENUE_DROP",
                "WARNING",
                "Revenue dropped",
                null,
                "Review pricing");

        var result = verifier.verify(insight, snapshot);

        assertFalse(result.accepted());
        assertEquals("evidence is missing", result.reason());
    }
}
