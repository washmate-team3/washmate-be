package swp391.carwash.service;

import swp391.carwash.dto.respone.vehicles.RedemptionResponse;
import swp391.carwash.dto.respone.vehicles.RewardResponse;

import java.util.List;

public interface RewardService {
    List<RewardResponse> getAvailableRewards(Integer garageId);
    RedemptionResponse redeemReward(Integer userId, Integer garageId, Integer rewardId);
}