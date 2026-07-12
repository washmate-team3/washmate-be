package swp391.carwash.dto.request.PromotionReward;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import swp391.carwash.enums.DiscountType;
import swp391.carwash.enums.RewardStatus;

import java.math.BigDecimal;

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

    private RewardStatus status;
    @NotBlank
    @Pattern(regexp = "PERCENTAGE|FIXED_AMOUNT")
    private DiscountType discountType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal discountValue;

    @DecimalMin("0.0")
    private BigDecimal maxDiscount;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal minOrderValue;

    @NotNull
    @Min(1)
    private Integer validDays;
}