package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.carwash.dto.request.Reward.RewardCreateRequest;
import swp391.carwash.dto.request.Reward.RewardUpdateRequest;
import swp391.carwash.dto.respone.Reward.RewardResponse;
import swp391.carwash.entity.Garage;
import swp391.carwash.entity.Reward;
import swp391.carwash.repository.GarageRepository;
import swp391.carwash.repository.RewardRepository;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private final RewardRepository rewardRepository;
    private final GarageRepository garageRepository;

    @Override
    public RewardResponse createReward(RewardCreateRequest request) {
// Dùng proxy reference để map FK garage_id chuẩn chỉ không sợ tốn câu lệnh SELECT
        Garage garageProxy = garageRepository.getReferenceById(request.garageId());

        Reward reward = Reward.builder()
                .garage(garageProxy)
                .name(request.name())
                .description(request.description())
                .pointsRequired(request.pointsRequired())
                .stock(request.stock())
                .status(request.stock() > 0 ? "ACTIVE" : "OUT_OF_STOCK")
                .build();

        return mapToResponse(rewardRepository.save(reward));    }

    @Override
    public RewardResponse getRewardById(Integer rewardId) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phần thưởng với ID: " + rewardId));
        return mapToResponse(reward);
    }

    @Override
    public RewardResponse updateReward(Integer rewardId, RewardUpdateRequest request) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phần thưởng với ID: " + rewardId));

        reward.setName(request.name());
        reward.setDescription(request.description());
        reward.setPointsRequired(request.pointsRequired());
        reward.setStock(request.stock());
        reward.setStatus(request.status());

        return mapToResponse(rewardRepository.save(reward));    }

    @Override
    public void deleteReward(Integer rewardId) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phần thưởng để xóa"));
        // Xóa mềm sang trạng thái DELETED để giữ vẹn toàn dữ liệu lịch sử
        reward.setStatus("DELETED");
        rewardRepository.save(reward);
    }

    @Override
    public Page<RewardResponse> getAllRewards(Integer garageId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return rewardRepository.findByGarageIdAndStatus(garageId, status, pageable).map(this::mapToResponse);
        }
        return rewardRepository.findByGarageIdAndStatusNot(garageId, "DELETED", pageable).map(this::mapToResponse);
    }
    private RewardResponse mapToResponse(Reward reward) {
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