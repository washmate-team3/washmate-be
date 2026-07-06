package swp391.carwash.dto.insight;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AIInsightEnrichResponse {
    private Integer insightId;
    private String aiSummary;
    private String aiExplanation;
    private List<String> aiRecommendation;
    private AICampaignSuggestion aiCampaignSuggestion;
    private BigDecimal confidenceScore;
    private String aiModel;
    private String promptVersion;
    private OffsetDateTime generatedAt;
}
