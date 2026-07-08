package swp391.carwash.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.carwash.dto.response.Reward.RewardRedemptionResponse;
import swp391.carwash.dto.response.Reward.RewardResponse;

public interface CustomerPromotionRewardService {

    Page<RewardResponse> getRedeemablePromotions(
            Integer garageId,
            Pageable pageable
    );

    RewardRedemptionResponse redeemPromotionReward(
            Integer userId,
            Integer garageId,
            Integer rewardId
    );

    Page<RewardRedemptionResponse> getMyPromotionRedemptions(
            Integer userId,
            Integer garageId,
            Pageable pageable
    );
}
