package swp391.carwash.dto.insight;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIInsightResult {
    private String aiSummary;
    private String aiExplanation;
    private List<String> aiRecommendation;
    private AICampaignSuggestion aiCampaignSuggestion;
    private BigDecimal confidenceScore;
}
