package swp391.carwash.dto.insight;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import swp391.carwash.enums.InsightSource;

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
    private InsightSource source;
    private Map<String, Object> evidence;
    private Boolean verified;
    private OffsetDateTime generatedAt;
}
