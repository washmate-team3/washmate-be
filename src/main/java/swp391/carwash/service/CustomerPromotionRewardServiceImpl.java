package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import swp391.carwash.dto.response.Reward.RewardRedemptionResponse;
import swp391.carwash.dto.response.Reward.RewardResponse;
import swp391.carwash.entity.*;
import swp391.carwash.enums.TransactionType;
import swp391.carwash.repository.*;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CustomerPromotionRewardServiceImpl implements CustomerPromotionRewardService {

    private static final String ACTIVE = "ACTIVE";
    private static final String PENDING = "PENDING";
    private static final String LOYALTY_UPDATE = "LOYALTY_UPDATE";
    private static final String IN_APP = "IN_APP";

    private final RewardRepository rewardRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final RewardRedemptionRepository redemptionRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final NotificationRepository notificationRepository;
    private final PromotionRepository promotionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<RewardResponse> getRedeemablePromotions(
            Integer garageId,
            Pageable pageable) {

        return rewardRepository
                .findByGarageIdAndPromotionIsNotNullAndStatus(
                        garageId,
                        ACTIVE,
                        pageable
                )
                .map(RewardResponse::from);
    }

    @Override
    @Transactional
    public RewardRedemptionResponse redeemPromotionReward(
            Integer userId,
            Integer garageId,
            Integer rewardId) {

        // 1. Lấy reward có gắn Promotion mẫu
        Reward reward = getPromotionReward(garageId, rewardId);

        // 2. Lấy tài khoản loyalty của customer
        LoyaltyAccount account = getAccount(userId, garageId);

        // 3. Kiểm tra điểm, stock, trạng thái reward
        validateRedeem(account, reward);

        // 4. Trừ availablePoints
        deductPoints(account, reward.getPointsRequired());

        // 5. Trừ stock reward
        deductStock(reward);

        // 6. Clone Promotion mẫu thành Promotion riêng cho customer
        Promotion issuedPromotion = issuePromotionForCustomer(
                reward.getPromotion(),
                garageId
        );

        // 7. Lưu redemption và liên kết Promotion vừa sinh
        RewardRedemption redemption = saveRedemption(
                account,
                reward,
                issuedPromotion
        );

        // 8. Ghi lịch sử trừ điểm
        saveRedeemTransaction(account, redemption);

        // 9. Thông báo mã giảm giá cho customer
        createNotification(account, redemption);

        return RewardRedemptionResponse.fromEntity(redemption);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RewardRedemptionResponse> getMyPromotionRedemptions(
            Integer userId,
            Integer garageId,
            Pageable pageable) {

        return redemptionRepository
                .findByLoyaltyAccountUserIdAndGarageIdOrderByRedeemedAtDesc(
                        userId,
                        garageId,
                        pageable
                )
                .map(RewardRedemptionResponse::fromEntity);
    }

    private Reward getPromotionReward(Integer garageId, Integer rewardId) {
        Reward reward = rewardRepository
                .findByRewardIdAndGarageId(rewardId, garageId)
                .orElseThrow(() -> new RuntimeException("Phần thưởng không tồn tại."));

        if (reward.getPromotion() == null) {
            throw new RuntimeException("Phần thưởng này không phải mã giảm giá.");
        }

        return reward;
    }

    private LoyaltyAccount getAccount(Integer userId, Integer garageId) {
        return loyaltyAccountRepository
                .findByUserIdAndGarageId(userId, garageId)
                .orElseThrow(() -> new RuntimeException("Tài khoản tích điểm không tồn tại."));
    }

    private void validateRedeem(LoyaltyAccount account, Reward reward) {
        if (!ACTIVE.equals(reward.getStatus())) {
            throw new RuntimeException("Mã giảm giá không còn hoạt động.");
        }

        if (reward.getStock() <= 0) {
            throw new RuntimeException("Mã giảm giá đã hết lượt đổi.");
        }

        if (account.getAvailablePoints() < reward.getPointsRequired()) {
            throw new RuntimeException("Bạn không đủ điểm để đổi mã giảm giá này.");
        }
    }

    private void deductPoints(LoyaltyAccount account, Integer points) {
        account.setAvailablePoints(account.getAvailablePoints() - points);
        account.setUpdatedAt(OffsetDateTime.now());
        loyaltyAccountRepository.save(account);
    }

    private void deductStock(Reward reward) {
        reward.setStock(reward.getStock() - 1);

        if (reward.getStock() == 0) {
            reward.setStatus("OUT_OF_STOCK");
        }

        rewardRepository.save(reward);
    }

    private RewardRedemption saveRedemption(
            LoyaltyAccount account,
            Reward reward,
            Promotion issuedPromotion) {

        ZonedDateTime now = ZonedDateTime.now();

        RewardRedemption redemption = RewardRedemption.builder()
                .loyaltyAccount(account)
                .garage(reward.getGarage())
                .reward(reward)

                // Lưu Promotion riêng vừa clone, không lưu Promotion mẫu
                .promotion(issuedPromotion)

                .pointsUsed(reward.getPointsRequired())
                .status("COMPLETED")
                .redeemedAt(now)
                .completedAt(now)
                .build();

        return redemptionRepository.save(redemption);
    }

    private void saveRedeemTransaction(
            LoyaltyAccount account,
            RewardRedemption redemption) {

        OffsetDateTime now = OffsetDateTime.now();

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .account(account)
                .redemptionId(redemption.getRedemptionId())
                .points(-redemption.getPointsUsed())
                .transactionType(TransactionType.REDEEM)
                .description("Đổi điểm lấy mã giảm giá: "
                        + redemption.getPromotion().getPromoCode())
                .earnedAt(now)
                .createdAt(now)
                .expired(false)
                .build();

        loyaltyTransactionRepository.save(transaction);
    }

    private void createNotification(
            LoyaltyAccount account,
            RewardRedemption redemption) {

        String promoCode = redemption.getPromotion().getPromoCode();

        Notification notification = Notification.builder()
                .userId(account.getUser().getId())
                .title("Đổi mã giảm giá thành công")
                .content("Bạn đã đổi thành công mã giảm giá "
                        + promoCode
                        + " với "
                        + redemption.getPointsUsed()
                        + " điểm.")
                .type(LOYALTY_UPDATE)
                .channel(IN_APP)
                .status(PENDING)
                .isRead(false)
                .createdAt(OffsetDateTime.now())
                .build();

        notificationRepository.save(notification);
    }
    private Promotion issuePromotionForCustomer(
            Promotion template,
            Integer garageId) {

        if (template == null) {
            throw badRequest("Reward này không chứa Promotion.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (!ACTIVE.equals(template.getStatus())) {
            throw badRequest("Promotion mẫu không còn hoạt động.");
        }

        if (template.getEndDate().isBefore(now)) {
            throw badRequest("Promotion mẫu đã hết hạn.");
        }

        Promotion issuedPromotion = Promotion.builder()
                .garageId(garageId)
                .promoCode(generatePromoCode(garageId))
                .discountValue(template.getDiscountValue())
                .discountType(template.getDiscountType())
                .maxDiscount(template.getMaxDiscount())
                .minOrderValue(template.getMinOrderValue())

                // Mã riêng chỉ được dùng 1 lần
                .usageLimit(1)
                .usedCount(0)

                .startDate(now)
                .endDate(template.getEndDate())
                .status(ACTIVE)
                .build();

        return promotionRepository.save(issuedPromotion);
    }private String generatePromoCode(Integer garageId) {

        String promoCode;

        do {
            String randomPart = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();

            promoCode = "WM-G" + garageId + "-" + randomPart;

        } while (promotionRepository.existsByPromoCode(promoCode));

        return promoCode;
    }
    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
