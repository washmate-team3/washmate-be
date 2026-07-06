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
public class AIChatResult {
    private String answer;
    private List<String> suggestedActions;
    private List<Integer> referencedInsightIds;
    private BigDecimal confidenceScore;
}
