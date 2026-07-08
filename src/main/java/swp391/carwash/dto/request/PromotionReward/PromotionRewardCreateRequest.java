package swp391.carwash.dto.request.PromotionReward;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionRewardCreateRequest {

    @NotNull
    private Integer garageId;

    @NotNull
    private Integer promotionId;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Min(1)
    private Integer pointsRequired;

    @NotNull
    @Min(0)
    private Integer stock;
}