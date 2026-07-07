package swp391.carwash.dto.insight;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AIDetectedInsight(
        String type,
        String severity,
        String claim,
        AiInsightEvidence evidence,
        String suggestedAction
) {
    public AIDetectedInsight(
            @JsonProperty("type") String type,
            @JsonProperty("severity") String severity,
            @JsonProperty("claim") String claim,
            @JsonProperty("evidence") AiInsightEvidence evidence,
            @JsonProperty("suggested_action") String suggestedAction) {
        this.type = type;
        this.severity = severity;
        this.claim = claim;
        this.evidence = evidence;
        this.suggestedAction = suggestedAction;
    }
}
