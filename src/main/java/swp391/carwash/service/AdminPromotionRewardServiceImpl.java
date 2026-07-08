package swp391.carwash.service;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.request.PromotionReward.PromotionRewardCreateRequest;
import swp391.carwash.dto.request.PromotionReward.PromotionRewardUpdateRequest;
import swp391.carwash.dto.response.Reward.RewardRedemptionResponse;
import swp391.carwash.dto.response.Reward.RewardResponse;
import swp391.carwash.entity.*;
import swp391.carwash.enums.TransactionType;
import swp391.carwash.repository.*;

@Service
@RequiredArgsConstructor
public class AdminPromotionRewardServiceImpl implements AdminPromotionRewardService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DELETED = "DELETED";
    private static final String OUT_OF_STOCK = "OUT_OF_STOCK";

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String COMPLETED = "COMPLETED";
    private static final String REJECTED = "REJECTED";

    private final RewardRepository rewardRepository;
    private final RewardRedemptionRepository redemptionRepository;
    private final GarageRepository garageRepository;
    private final PromotionRepository promotionRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Override
    @Transactional
    public RewardResponse create(PromotionRewardCreateRequest request) {
        Garage garage = garageRepository.findById(request.getGarageId())
                .orElseThrow(() -> new RuntimeException("Garage không tồn tại."));

        Promotion promotion = getPromotionInGarage(
                request.getPromotionId(),
                request.getGarageId()
        );

        Reward reward = Reward.builder()
                .garage(garage)
                .promotion(promotion)
                .name(request.getName())
                .description(request.getDescription())
                .pointsRequired(request.getPointsRequired())
                .stock(request.getStock())
                .status(request.getStock() == 0 ? OUT_OF_STOCK : ACTIVE)
                .build();

        return RewardResponse.from(rewardRepository.save(reward));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RewardResponse> getAll(
            Integer garageId,
            String status,
            Pageable pageable) {

        Page<Reward> rewards = status == null
                ? rewardRepository.findByGarageIdAndPromotionIsNotNullAndStatusNot(
                garageId,
                DELETED,
                pageable
        )
                : rewardRepository.findByGarageIdAndPromotionIsNotNullAndStatus(
                garageId,
                status,
                pageable
        );

        return rewards.map(RewardResponse::from);
    }

    @Override
    @Transactional
    public RewardResponse update(
            Integer rewardId,
            PromotionRewardUpdateRequest request) {

        Reward reward = getPromotionReward(rewardId);

        if (request.getPromotionId() != null) {
            Promotion promotion = getPromotionInGarage(
                    request.getPromotionId(),
                    reward.getGarage().getId()
            );
            reward.setPromotion(promotion);
        }

        if (request.getName() != null) {
            reward.setName(request.getName());
        }

        if (request.getDescription() != null) {
            reward.setDescription(request.getDescription());
        }

        if (request.getPointsRequired() != null) {
            reward.setPointsRequired(request.getPointsRequired());
        }

        if (request.getStock() != null) {
            reward.setStock(request.getStock());

            if (request.getStock() == 0) {
                reward.setStatus(OUT_OF_STOCK);
            } else if (OUT_OF_STOCK.equals(reward.getStatus())) {
                reward.setStatus(ACTIVE);
            }
        }

        if (request.getStatus() != null) {
            reward.setStatus(request.getStatus());
        }

        return RewardResponse.from(rewardRepository.save(reward));
    }

    @Override
    @Transactional
    public void delete(Integer rewardId) {
        Reward reward = getPromotionReward(rewardId);
        reward.setStatus(DELETED);
        rewardRepository.save(reward);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RewardRedemptionResponse> getRedemptions(
            Integer garageId,
            String status,
            Pageable pageable) {

        Page<RewardRedemption> redemptions = status == null
                ? redemptionRepository.findByGarageIdAndRewardPromotionIsNotNull(
                garageId,
                pageable
        )
                : redemptionRepository.findByGarageIdAndRewardPromotionIsNotNullAndStatus(
                garageId,
                status,
                pageable
        );

        return redemptions.map(RewardRedemptionResponse::fromEntity);
    }

    private Promotion getPromotionInGarage(
            Integer promotionId,
            Integer garageId) {

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new RuntimeException("Promotion không tồn tại."));

        if (!promotion.getGarageId().equals(garageId)) {
            throw new RuntimeException("Promotion không thuộc garage này.");
        }

        return promotion;
    }

    private Reward getPromotionReward(Integer rewardId) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Promotion reward không tồn tại."));

        if (reward.getPromotion() == null) {
            throw new RuntimeException("Reward này không phải promotion reward.");
        }

        return reward;
    }

    private RewardRedemption getRedemption(Integer redemptionId) {
        RewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new RuntimeException("Redemption không tồn tại."));

        if (redemption.getReward().getPromotion() == null) {
            throw new RuntimeException("Redemption này không thuộc promotion reward.");
        }

        return redemption;
    }

    private void refundPoints(RewardRedemption redemption) {
        LoyaltyAccount account = redemption.getLoyaltyAccount();

        account.setAvailablePoints(
                account.getAvailablePoints() + redemption.getPointsUsed()
        );
        account.setUpdatedAt(OffsetDateTime.now());

        loyaltyAccountRepository.save(account);
    }

    private void restoreStock(Reward reward) {
        reward.setStock(reward.getStock() + 1);

        if (OUT_OF_STOCK.equals(reward.getStatus())) {
            reward.setStatus(ACTIVE);
        }

        rewardRepository.save(reward);
    }

    private void saveRefundTransaction(RewardRedemption redemption) {
        OffsetDateTime now = OffsetDateTime.now();

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .account(redemption.getLoyaltyAccount())
                .redemptionId(redemption.getRedemptionId())
                .points(redemption.getPointsUsed())
                .transactionType(TransactionType.REFUND)
                .description("Hoàn điểm do từ chối redemption: "
                        + redemption.getReward().getName())
                .earnedAt(now)
                .createdAt(now)
                .expired(false)
                .build();

        loyaltyTransactionRepository.save(transaction);
    }
}