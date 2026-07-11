package swp391.carwash.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
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

    @Override
    @Transactional
    public RewardResponse create(PromotionRewardCreateRequest request) {

        Garage garage = garageRepository.findById(request.getGarageId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Garage không tồn tại."
                ));

        validateCreateRequest(request);

        Promotion promotion = createPromotion(request);

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
    private Promotion createPromotion(
            PromotionRewardCreateRequest request) {

        Promotion promotion = Promotion.builder()
                .garageId(request.getGarageId())
                .promoCode(generatePromoCode(request.getGarageId()))
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscount(request.getMaxDiscount())
                .minOrderValue(request.getMinOrderValue())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ACTIVE)
                .build();

        return promotionRepository.save(promotion);
    }
    private void validateCreateRequest(
            PromotionRewardCreateRequest request) {

        if (request.getEndDate().isBefore(request.getStartDate())
                || request.getEndDate().isEqual(request.getStartDate())) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian kết thúc phải sau thời gian bắt đầu."
            );
        }

        if (!"PERCENTAGE".equals(request.getDiscountType())
                && !"FIXED_AMOUNT".equals(request.getDiscountType())) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Loại giảm giá không hợp lệ."
            );
        }

        if ("PERCENTAGE".equals(request.getDiscountType())
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Phần trăm giảm giá không được vượt quá 100."
            );
        }
    }
    private String generatePromoCode(Integer garageId) {

        String promoCode;

        do {

            String randomPart = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();

            promoCode = "WM-G"
                    + garageId
                    + "-"
                    + randomPart;

        } while (promotionRepository.existsByPromoCode(promoCode));

        return promoCode;
    }
}