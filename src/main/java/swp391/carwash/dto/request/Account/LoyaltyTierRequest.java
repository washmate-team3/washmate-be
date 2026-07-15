package swp391.carwash.dto.request.Account;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTierRequest {

    @NotBlank(message = "Tier name is required")
    private String tierName;

    @NotNull(message = "Minimum points is required")
    @Min(value = 0, message = "Minimum points must be greater than or equal to 0")
    private Integer minPoints;

    @NotNull(message = "Maintain points is required")
    @Min(value = 0, message = "Maintain points must be greater than or equal to 0")
    private Integer maintainPoints;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    private BigDecimal discountPercentage;

    // Số ngày được đặt lịch trước của hạng. Bỏ trống = dùng mặc định hệ thống.
    @Min(value = 1, message = "Advance booking days must be at least 1")
    private Integer advanceBookingDays;
}