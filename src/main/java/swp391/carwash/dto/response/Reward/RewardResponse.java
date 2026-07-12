package swp391.carwash.dto.response.Reward;


import swp391.carwash.entity.Reward;
import swp391.carwash.enums.DiscountType;
import swp391.carwash.enums.RewardStatus;

import java.math.BigDecimal;

public record RewardResponse(
        Integer rewardId,
        Integer garageId,
        String name,
        String description,
        Integer pointsRequired,
        Integer stock,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscount,
        BigDecimal minOrderValue,
        Integer validDays,
        RewardStatus status
) {

    public static RewardResponse from(Reward reward) {
        return new RewardResponse(
                reward.getRewardId(),
                reward.getGarage().getId(),
                reward.getName(),
                reward.getDescription(),
                reward.getPointsRequired(),
                reward.getStock(),
                reward.getDiscountType(),
                reward.getDiscountValue(),
                reward.getMaxDiscount(),
                reward.getMinOrderValue(),
                reward.getValidDays(),
                reward.getStatus()
        );
    }
}