package swp391.carwash.dto.request.PromotionReward;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import swp391.carwash.enums.DiscountType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRewardCreateRequest {

    @NotNull
    private Integer garageId;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Min(1)
    private Integer pointsRequired;

    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull(message = "Loại giảm giá không được để trống")
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