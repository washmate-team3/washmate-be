package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.LoyaltyAccountResponse;
import swp391.carwash.dto.LoyaltyTransactionResponse;
import swp391.carwash.dto.response.CustomerLoyaltySummaryResponse;
import swp391.carwash.dto.response.Loyaty.LoyaltyPolicyResponse;
import swp391.carwash.dto.response.vehicles.LoyaltyTierResponse;
import swp391.carwash.entity.LoyaltyAccount;
import swp391.carwash.entity.LoyaltyPolicy;
import swp391.carwash.entity.MembershipTier;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.enums.TransactionType;
import swp391.carwash.repository.LoyaltyAccountRepository;
import swp391.carwash.repository.LoyaltyPolicyRepository;
import swp391.carwash.repository.LoyaltyTransactionRepository;
import swp391.carwash.repository.MembershipTierRepository;
import swp391.carwash.service.Loyalty.Support.QuarterCalculator;
import swp391.carwash.service.Loyalty.Support.QuarterPeriod;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerLoyaltyServiceImpl implements CustomerLoyaltyService {


    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final LoyaltyPolicyRepository loyaltyPolicyRepository;
    private final QuarterCalculator quarterCalculator;

    @Override
    @Transactional(readOnly = true)
    public LoyaltyAccountResponse getMyLoyalty(Integer userId) {

        LoyaltyAccount account = loyaltyAccountRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("Loyalty account not found."));

        return LoyaltyAccountResponse.from(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoyaltyTierResponse> getTiers(Integer garageId) {

        return membershipTierRepository
                .findByGarageIdAndStatusOrderByMinPointsAsc(
                        garageId,
                        RecordStatus.ACTIVE)
                .stream()
                .map(this::toTierResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponse> getTransactions(Integer userId) {

        LoyaltyAccount account = loyaltyAccountRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("Loyalty account not found."));

        return loyaltyTransactionRepository
                .findByAccountUserIdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(LoyaltyTransactionResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LoyaltyPolicyResponse getPolicy(Integer garageId) {

        LoyaltyPolicy policy = loyaltyPolicyRepository
                .findByGarageIdAndStatus(
                        garageId,
                        RecordStatus.ACTIVE)
                .orElseThrow(() ->
                        new RuntimeException("Loyalty policy not found."));

        return toPolicyResponse(policy);
    }

    @Transactional(readOnly = true)
    public CustomerLoyaltySummaryResponse getSummary(
            Integer userId,
            Integer garageId) {

        LoyaltyAccount account = loyaltyAccountRepository
                .findByUserIdAndGarageId(userId, garageId)
                .orElseThrow(() -> new RuntimeException("Tài khoản tích điểm không tồn tại."));

        MembershipTier currentTier = account.getTier();

        QuarterPeriod period = quarterCalculator.currentQuarter();

        int earnedPoint = loyaltyTransactionRepository.sumEarnPoint(
                account.getId(),
                TransactionType.EARN,
                period.start(),
                period.end()
        );

        MembershipTier nextTier = membershipTierRepository
                .findFirstByGarageIdAndStatusAndMinPointsGreaterThanOrderByMinPointsAsc(
                        garageId,
                        RecordStatus.ACTIVE,
                        currentTier.getMinPoints())
                .orElse(null);

        int pointsNeededToMaintain =
                Math.max(0, currentTier.getMaintainPoints() - earnedPoint);

        boolean canMaintain =
                earnedPoint >= currentTier.getMaintainPoints();

        Integer nextTierMinPoints =
                nextTier == null ? null : nextTier.getMinPoints();

        Integer pointsNeededToUpgrade =
                nextTier == null
                        ? 0
                        : Math.max(0, nextTier.getMinPoints() - account.getTotalPoints());

        boolean canUpgrade =
                nextTier != null && account.getTotalPoints() >= nextTier.getMinPoints();

        int upgradeProgressPercent =
                calculateUpgradeProgress(account, nextTier);

        return CustomerLoyaltySummaryResponse.builder()
                .garageId(garageId)
                .currentTierName(currentTier.getTierName())
                .totalPoints(account.getTotalPoints())
                .availablePoints(account.getAvailablePoints())
                .currentQuarterEarnedPoints(earnedPoint)
                .maintainPoints(currentTier.getMaintainPoints())
                .pointsNeededToMaintain(pointsNeededToMaintain)
                .canMaintain(canMaintain)
                .nextTierName(nextTier == null ? null : nextTier.getTierName())
                .nextTierMinPoints(nextTierMinPoints)
                .pointsNeededToUpgrade(pointsNeededToUpgrade)
                .canUpgrade(canUpgrade)
                .upgradeProgressPercent(upgradeProgressPercent)
                .build();
    }

    private LoyaltyTierResponse toTierResponse(MembershipTier tier) {

        return LoyaltyTierResponse.builder()
                .tierId(tier.getId())
                .garageId(tier.getGarage().getId())
                .tierName(tier.getTierName())
                .minPoints(tier.getMinPoints())
                .maintainPoints(tier.getMaintainPoints())
                .discountPercentage(tier.getDiscountPercentage())
                .status(tier.getStatus())
                .build();
    }
    private LoyaltyPolicyResponse toPolicyResponse(LoyaltyPolicy policy) {

        return LoyaltyPolicyResponse.builder()
                .policyId(policy.getId())
                .garageId(policy.getGarage().getId())
                .amountPerPoint(policy.getAmountPerPoint())
                .pointExpiryMonths(policy.getPointExpiryMonths())
                .autoEnroll(policy.getAutoEnroll())
                .build();
    }
    private int calculateUpgradeProgress(
            LoyaltyAccount account,
            MembershipTier nextTier) {

        if (nextTier == null) {
            return 100;
        }

        if (nextTier.getMinPoints() <= 0) {
            return 100;
        }

        int progress = account.getTotalPoints() * 100 / nextTier.getMinPoints();

        return Math.min(progress, 100);
    }
}