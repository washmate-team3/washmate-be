package swp391.carwash.dto.insight;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record AiInsightEvidence(
        String metric,
        BigDecimal value,
        String period
) {
    public AiInsightEvidence(
            @JsonProperty("metric") String metric,
            @JsonProperty("value") BigDecimal value,
            @JsonProperty("period") String period) {
        this.metric = metric;
        this.value = value;
        this.period = period;
    }
}
