package swp391.carwash.dto.insight;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AIChatResponse {
    private String answer;
    private List<String> suggestedActions;
    private List<Integer> referencedInsightIds;
    private BigDecimal confidenceScore;
    private String aiModel;
    private String promptVersion;
    private OffsetDateTime generatedAt;
}
