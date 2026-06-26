package swp391.carwash.dto.respone.Reward;

import swp391.carwash.entity.RewardRedemption;

import java.time.ZonedDateTime;


public record RewardRedemptionResponse(
        Integer redemptionId,
        Integer loyaltyAccountId,
        Integer rewardId,
        String rewardName, // Trả thêm tên quà để FE hiển thị luôn không cần gọi API tìm tên
        Integer pointsUsed,
        String status, // PENDING, APPROVED, COMPLETED, REJECTED
        ZonedDateTime redeemedAt
) {
    public static RewardRedemptionResponse fromEntity(RewardRedemption r) {
        return new RewardRedemptionResponse(
                r.getRedemptionId(),
                r.getLoyaltyAccount().getId(),
                r.getReward().getRewardId(),
                r.getReward().getName(),
                r.getPointsUsed(),
                r.getStatus(),
                r.getRedeemedAt()
        );
    }


}