package swp391.carwash.dto.response.Reward;


import swp391.carwash.entity.Reward;

public record RewardResponse(
        Integer rewardId,
        Integer garageId,
        String name,
        String description,
        Integer pointsRequired,
        Integer stock,
        String status
) {
    public static RewardResponse from(Reward reward) {
        return new RewardResponse(
                reward.getRewardId(),
                reward.getGarage().getId(),
                reward.getName(),
                reward.getDescription(),
                reward.getPointsRequired(),
                reward.getStock(),
                reward.getStatus()
        );
    }
}