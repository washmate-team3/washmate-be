package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.response.Reward.RewardRedemptionResponse;
import swp391.carwash.dto.response.Reward.RewardResponse;
import swp391.carwash.entity.*;
import swp391.carwash.enums.PromotionStatus;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.enums.RewardStatus;
import swp391.carwash.enums.TransactionType;
import swp391.carwash.repository.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerPromotionRewardServiceImpl
        implements CustomerPromotionRewardService {

    private static final String ACTIVE = "ACTIVE";
    private static final String OUT_OF_STOCK = "OUT_OF_STOCK";
    private static final String COMPLETED = "COMPLETED";

    private static final String PENDING = "PENDING";
    private static final String LOYALTY_UPDATE = "LOYALTY_UPDATE";
    private static final String IN_APP = "IN_APP";

    private final RewardRepository rewardRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final RewardRedemptionRepository redemptionRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final PromotionRepository promotionRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<RewardResponse> getRedeemablePromotions(
            Integer garageId,
            Pageable pageable
    ) {
        return rewardRepository
                .findByGarageIdAndStatusAndStockGreaterThan(
                        garageId,
                        ACTIVE,
                        0,
                        pageable
                )
                .map(RewardResponse::from);
    }

    @Override
    @Transactional
    public RewardRedemptionResponse redeemPromotionReward(
            Integer userId,
            Integer garageId,
            Integer rewardId
    ) {
        Reward reward = rewardRepository
                .findByIdForUpdate(rewardId)
                .orElseThrow(() -> notFound(
                        "Phần thưởng không tồn tại."
                ));

        validateGarage(reward, garageId);

        LoyaltyAccount account = loyaltyAccountRepository
                .findForRedeem(
                        userId,
                        garageId,
                        RecordStatus.ACTIVE
                )
                .orElseThrow(() -> notFound(
                        "Tài khoản tích điểm không tồn tại."
                ));

        validateRedeem(account, reward);

        deductPoints(account, reward.getPointsRequired());
        deductStock(reward);

        Promotion promotion = createPersonalPromotion(reward);

        RewardRedemption redemption = createRedemption(
                account,
                reward,
                promotion
        );

        createTransaction(account, redemption);
        createNotification(account, redemption);

        return RewardRedemptionResponse.fromEntity(redemption);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RewardRedemptionResponse> getMyPromotionRedemptions(
            Integer userId,
            Integer garageId,
            Pageable pageable
    ) {
        return redemptionRepository
                .findByLoyaltyAccountUserIdAndGarageIdOrderByRedeemedAtDesc(
                        userId,
                        garageId,
                        pageable
                )
                .map(RewardRedemptionResponse::fromEntity);
    }

    private void validateGarage(
            Reward reward,
            Integer garageId
    ) {
        if (!reward.getGarage().getId().equals(garageId)) {
            throw badRequest(
                    "Phần thưởng không thuộc garage này."
            );
        }
    }

    private void validateRedeem(
            LoyaltyAccount account,
            Reward reward
    ) {
        if (!ACTIVE.equals(reward.getStatus())) {
            throw badRequest(
                    "Phần thưởng hiện không thể đổi."
            );
        }

        if (reward.getStock() == null
                || reward.getStock() <= 0) {
            throw badRequest(
                    "Phần thưởng đã hết lượt đổi."
            );
        }

        if (account.getAvailablePoints() == null
                || account.getAvailablePoints()
                < reward.getPointsRequired()) {
            throw badRequest(
                    "Bạn không đủ điểm để đổi phần thưởng."
            );
        }
    }

    private void deductPoints(
            LoyaltyAccount account,
            Integer points
    ) {
        account.setAvailablePoints(
                account.getAvailablePoints() - points
        );

        account.setUpdatedAt(OffsetDateTime.now());
    }

    private void deductStock(Reward reward) {
        int remainingStock = reward.getStock() - 1;

        reward.setStock(remainingStock);

        if (remainingStock == 0) {
            reward.setStatus(RewardStatus.OUT_OF_STOCK);
        }
    }

    private Promotion createPersonalPromotion(Reward reward) {
        OffsetDateTime now = OffsetDateTime.now();

        Promotion promotion = Promotion.builder()
                .garageId(reward.getGarage().getId())
                .promoCode(generatePromoCode(
                        reward.getGarage().getId()
                ))
                .discountType(reward.getDiscountType())
                .discountValue(reward.getDiscountValue())
                .maxDiscount(reward.getMaxDiscount())
                .minOrderValue(reward.getMinOrderValue())
                .usageLimit(1)
                .usedCount(0)
                .startDate(now)
                .endDate(
                        now.plusDays(reward.getValidDays())
                )
                .status(PromotionStatus.ACTIVE)
                .build();

        return promotionRepository.save(promotion);
    }

    private RewardRedemption createRedemption(
            LoyaltyAccount account,
            Reward reward,
            Promotion promotion
    ) {
        OffsetDateTime now = OffsetDateTime.now();

        RewardRedemption redemption =
                RewardRedemption.builder()
                        .loyaltyAccount(account)
                        .garage(reward.getGarage())
                        .reward(reward)
                        .promotion(promotion)
                        .pointsUsed(
                                reward.getPointsRequired()
                        )
                        .status(COMPLETED)
                        .redeemedAt(now)
                        .approvedAt(now)
                        .completedAt(now)
                        .build();

        return redemptionRepository.save(redemption);
    }

    private void createTransaction(
            LoyaltyAccount account,
            RewardRedemption redemption
    ) {
        LoyaltyTransaction transaction =
                LoyaltyTransaction.builder()
                        .account(account)
                        .redemptionId(
                                redemption.getRedemptionId()
                        )
                        .points(
                                -redemption.getPointsUsed()
                        )
                        .transactionType(
                                TransactionType.REDEEM
                        )
                        .description(
                                "Đổi điểm lấy mã giảm giá: "
                                        + redemption
                                        .getPromotion()
                                        .getPromoCode()
                        )
                        .earnedAt(OffsetDateTime.now())
                        .createdAt(OffsetDateTime.now())
                        .expired(false)
                        .build();

        transactionRepository.save(transaction);
    }

    private void createNotification(
            LoyaltyAccount account,
            RewardRedemption redemption
    ) {
        Notification notification =
                Notification.builder()
                        .userId(account.getUser().getId())
                        .title(
                                "Đổi mã giảm giá thành công"
                        )
                        .content(
                                "Mã của bạn: "
                                        + redemption
                                        .getPromotion()
                                        .getPromoCode()
                        )
                        .type(LOYALTY_UPDATE)
                        .channel(IN_APP)
                        .status(PENDING)
                        .isRead(false)
                        .createdAt(
                                OffsetDateTime.now()
                        )
                        .build();

        notificationRepository.save(notification);
    }

    private String generatePromoCode(Integer garageId) {
        String promoCode;

        do {
            String randomPart = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();

            promoCode = "WM-G"
                    + garageId
                    + "-"
                    + randomPart;

        } while (
                promotionRepository.existsByPromoCode(
                        promoCode
                )
        );

        return promoCode;
    }

    private ApiException notFound(String message) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    private ApiException badRequest(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}