package swp391.carwash.dto.respone.Redem;

import java.time.ZonedDateTime;

public record RewardRedemptionResponse(
        Integer redemptionId,
        Integer loyaltyAccountId,
        Integer rewardId,
        String rewardName, // Trả thêm tên quà để FE hiển thị luôn không cần gọi API tìm tên
        Integer pointsUsed,
        String status, // PENDING, APPROVED, COMPLETED, REJECTED
        ZonedDateTime redeemedAt
) {}