package swp391.carwash.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.carwash.dto.request.PromotionReward.PromotionRewardCreateRequest;
import swp391.carwash.dto.request.PromotionReward.PromotionRewardUpdateRequest;
import swp391.carwash.dto.response.Reward.RewardRedemptionResponse;
import swp391.carwash.dto.response.Reward.RewardResponse;

public interface AdminPromotionRewardService {

    RewardResponse create(
            PromotionRewardCreateRequest request
    );

    Page<RewardResponse> getAll(
            Integer garageId,
            String status,
            Pageable pageable
    );

    RewardResponse update(
            Integer rewardId,
            PromotionRewardUpdateRequest request
    );

    void delete(Integer rewardId);

    Page<RewardRedemptionResponse> getRedemptions(
            Integer garageId,
            String status,
            Pageable pageable
    );
}