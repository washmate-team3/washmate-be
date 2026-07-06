package swp391.carwash.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.carwash.dto.request.Reward.RewardCreateRequest;
import swp391.carwash.dto.request.Reward.RewardUpdateRequest;
import swp391.carwash.dto.response.Reward.RewardResponse;

public interface RewardService {
    RewardResponse createReward(RewardCreateRequest request);
    RewardResponse getRewardById(Integer rewardId);
    RewardResponse updateReward(Integer rewardId, RewardUpdateRequest request);
    void deleteReward(Integer rewardId);
    Page<RewardResponse> getAllRewards(Integer garageId, String status, Pageable pageable);
}