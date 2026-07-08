package swp391.carwash.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerLoyaltySummaryResponse {

    private Integer garageId;

    private String currentTierName;
    private Integer totalPoints;
    private Integer availablePoints;

    private Integer currentQuarterEarnedPoints;

    private Integer maintainPoints;
    private Integer pointsNeededToMaintain;
    private Boolean canMaintain;

    private String nextTierName;
    private Integer nextTierMinPoints;
    private Integer pointsNeededToUpgrade;
    private Boolean canUpgrade;

    private Integer upgradeProgressPercent;
}