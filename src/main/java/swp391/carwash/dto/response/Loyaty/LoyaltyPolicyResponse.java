package swp391.carwash.dto.response.Loyaty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPolicyResponse {

    private Integer policyId;

    private Integer garageId;

    private BigDecimal amountPerPoint;

    private Integer pointExpiryMonths;

    private Boolean autoEnroll;
}