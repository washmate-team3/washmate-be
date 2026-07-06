package swp391.carwash.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.carwash.entity.LoyaltyAccount;
import swp391.carwash.enums.RecordStatus;

public record LoyaltyAccountResponse(
        Integer accountId,
        Integer garageId,
        String garageName,
        Integer tierId,
        String tierName,
        Integer tierMinPoints,
        BigDecimal tierDiscountPercentage,
        Integer totalPoints,
        Integer availablePoints,
        RecordStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static LoyaltyAccountResponse from(LoyaltyAccount account) {
        return new LoyaltyAccountResponse(
                account.getId(),
                account.getGarage().getId(),
                account.getGarage().getName(),
                account.getTier().getId(),
                account.getTier().getTierName(),
                account.getTier().getMinPoints(),
                account.getTier().getDiscountPercentage(),
                account.getTotalPoints(),
                account.getAvailablePoints(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}

