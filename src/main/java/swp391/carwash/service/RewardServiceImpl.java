package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.respone.vehicles.RedemptionResponse;
import swp391.carwash.dto.respone.vehicles.RewardResponse;
import swp391.carwash.entity.Reward;
import swp391.carwash.repository.RewardRepository;
import swp391.carwash.service.RewardService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private final RewardRepository rewardRepository;

    @Override
    public List<RewardResponse> getAvailableRewards(Integer garageId) {
        return List.of();
    }

    @Override
    public RedemptionResponse redeemReward(Integer userId, Integer garageId, Integer rewardId) {
        return null;
    }

    @Override
    @Transactional
    public RewardResponse createReward(RewardCreateRequest request) {
        Reward reward = Reward.builder()
                .garageId(request.garageId())
                .name(request.name())
                .description(request.description())
                .pointsRequired(request.pointsRequired())
                .stock(request.stock())
                .status(request.stock() > 0 ? RewardStatus.ACTIVE.name() : RewardStatus.OUT_OF_STOCK.name())
                .build();

        Reward savedReward = rewardRepository.save(reward);
        return mapToResponse(savedReward);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RewardResponse> getAllRewards(Integer garageId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return rewardRepository.findByGarageIdAndStatus(garageId, status, pageable).map(this::mapToResponse);
        }
        // Loại bỏ các quà đã bị xóa (DELETED) khỏi danh sách quản lý thông thường
        return rewardRepository.findByGarageIdAndStatusNot(garageId, RewardStatus.DELETED.name(), pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RewardResponse> getActiveRewardsByGarage(Integer garageId, Pageable pageable) {
        // Khách hàng chỉ xem được quà đang ACTIVE và còn hàng trong kho
        return rewardRepository.findByGarageIdAndStatusAndStockGreaterThan(
                garageId, RewardStatus.ACTIVE.name(), 0, pageable
        ).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RewardResponse getRewardById(Integer rewardId) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phần thưởng với ID: " + rewardId));
        return mapToResponse(reward);
    }

    @Override
    @Transactional
    public RewardResponse updateReward(Integer rewardId, RewardUpdateRequest request) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phần thưởng với ID: " + rewardId));

        reward.setName(request.name());
        reward.setDescription(request.description());
        reward.setPointsRequired(request.pointsRequired());
        reward.setStock(request.stock());

        // Tự động cập nhật trạng thái dựa trên số lượng kho nếu trạng thái truyền vào là ACTIVE
        if (RewardStatus.ACTIVE.name().equals(request.status()) && request.stock() <= 0) {
            reward.setStatus(RewardStatus.OUT_OF_STOCK.name());
        } else {
            reward.setStatus(request.status());
        }

        Reward updatedReward = rewardRepository.save(reward);
        return mapToResponse(updatedReward);
    }

    @Override
    @Transactional
    public void deleteReward(Integer rewardId) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phần thưởng để xóa"));

        // Không xóa cứng vật lý khỏi DB để giữ vẹn toàn dữ liệu lịch sử đổi quà, chuyển trạng thái thành DELETED
        reward.setStatus(RewardStatus.DELETED.name());
        rewardRepository.save(reward);
    }

    private RewardResponse mapToResponse(Reward reward) {
        return new RewardResponse(
                reward.getRewardId(),
                reward.getGarageId(),
                reward.getName(),
                reward.getDescription(),
                reward.getPointsRequired(),
                reward.getStock(),
                reward.getStatus()
        );
    }
}