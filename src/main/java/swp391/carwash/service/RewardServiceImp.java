package swp391.carwash.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import swp391.carwash.dto.respone.vehicles.RedemptionResponse;
import swp391.carwash.dto.respone.vehicles.RewardResponse;
import swp391.carwash.repository.*;
import swp391.carwash.entity.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewardServiceImp implements RewardService {

    @Override
    public List<RewardResponse> getAvailableRewards(Integer garageId) {
        return List.of();
    }

    @Override
    public RedemptionResponse redeemReward(Integer userId, Integer garageId, Integer rewardId) {
        return null;
    }
}