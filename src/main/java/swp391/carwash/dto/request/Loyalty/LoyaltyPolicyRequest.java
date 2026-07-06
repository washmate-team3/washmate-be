package swp391.carwash.dto.request.Loyalty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPolicyRequest {

    @NotNull(message = "Amount per point is required")
    @DecimalMin(value = "1.0", message = "Amount per point must be greater than 0")
    private BigDecimal amountPerPoint;

    @NotNull(message = "Point expiry months is required")
    @Min(value = 1, message = "Point expiry months must be at least 1")
    private Integer pointExpiryMonths;

    @NotNull(message = "Auto enroll is required")
    private Boolean autoEnroll;
}