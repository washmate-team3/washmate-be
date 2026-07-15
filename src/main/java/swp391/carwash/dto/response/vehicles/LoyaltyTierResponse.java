package swp391.carwash.dto.response.vehicles;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.carwash.enums.RecordStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTierResponse {

    private Integer tierId;

    private Integer garageId;

    private String tierName;

    private Integer minPoints;

    private Integer maintainPoints;

    private BigDecimal discountPercentage;

    private Integer advanceBookingDays;

    private RecordStatus status;
}