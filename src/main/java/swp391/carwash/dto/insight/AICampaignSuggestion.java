package swp391.carwash.dto.insight;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AICampaignSuggestion {
    private String campaignName;
    private String targetCustomers;
    private String offer;
    private String duration;
    private String goal;
}
