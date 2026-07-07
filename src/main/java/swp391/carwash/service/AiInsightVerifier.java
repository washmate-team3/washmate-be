package swp391.carwash.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import swp391.carwash.dto.insight.AIDetectedInsight;
import swp391.carwash.service.insight.MetricSnapshot;

@Service
public class AiInsightVerifier {
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    public VerificationResult verify(AIDetectedInsight insight, MetricSnapshot snapshot) {
        if (insight == null) {
            return VerificationResult.rejected("insight is missing");
        }
        if (insight.evidence() == null) {
            return VerificationResult.rejected("evidence is missing");
        }
        if (!StringUtils.hasText(insight.evidence().metric())) {
            return VerificationResult.rejected("evidence metric is missing");
        }
        if (insight.evidence().value() == null) {
            return VerificationResult.rejected("evidence value is missing");
        }

        BigDecimal dbValue = snapshot.valueOf(insight.evidence().metric());
        if (dbValue == null) {
            return VerificationResult.rejected("metric does not exist in snapshot");
        }
        BigDecimal delta = dbValue.subtract(insight.evidence().value()).abs();
        if (delta.compareTo(TOLERANCE) > 0) {
            return VerificationResult.rejected("AI evidence value does not match backend metric");
        }

        Map<String, Object> evidenceJson = new LinkedHashMap<>();
        evidenceJson.put("metric", insight.evidence().metric());
        evidenceJson.put("value", dbValue);
        evidenceJson.put("period", insight.evidence().period());
        evidenceJson.put("verified", true);
        evidenceJson.put("tolerance", TOLERANCE);
        return VerificationResult.accepted(dbValue, evidenceJson);
    }

    public record VerificationResult(
            boolean accepted,
            String reason,
            BigDecimal dbValue,
            Map<String, Object> evidenceJson
    ) {
        private static VerificationResult accepted(BigDecimal dbValue, Map<String, Object> evidenceJson) {
            return new VerificationResult(true, null, dbValue, evidenceJson);
        }

        private static VerificationResult rejected(String reason) {
            return new VerificationResult(false, reason, null, Map.of());
        }
    }
}
