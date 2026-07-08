package swp391.carwash.dto.request.PromotionReward;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionRewardUpdateRequest {

    private Integer promotionId;

    private String name;

    private String description;

    @Min(1)
    private Integer pointsRequired;

    @Min(0)
    private Integer stock;

    private String status;
}