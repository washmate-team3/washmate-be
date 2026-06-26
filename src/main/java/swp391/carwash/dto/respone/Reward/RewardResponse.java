package swp391.carwash.dto.respone.Reward;

public record RewardResponse(
        Integer rewardId,
        Integer garageId,
        String name,
        String description,
        Integer pointsRequired,
        Integer stock,
        String status // ACTIVE, INACTIVE, OUT_OF_STOCK, DELETED
) {}