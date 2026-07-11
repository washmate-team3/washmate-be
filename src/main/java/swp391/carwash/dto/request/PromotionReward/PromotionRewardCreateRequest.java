package swp391.carwash.dto.request.PromotionReward;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

    @NotBlank
    private String discountType;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal discountValue;

    private BigDecimal maxDiscount;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal minOrderValue;

    private Integer usageLimit;

    @NotNull
    private OffsetDateTime startDate;

    @NotNull
    private OffsetDateTime endDate;
}