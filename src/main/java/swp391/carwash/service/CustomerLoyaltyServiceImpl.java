package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.LoyaltyAccountResponse;
import swp391.carwash.dto.LoyaltyTransactionResponse;
import swp391.carwash.dto.response.Loyaty.LoyaltyPolicyResponse;
import swp391.carwash.dto.response.vehicles.LoyaltyTierResponse;
import swp391.carwash.entity.LoyaltyAccount;
import swp391.carwash.entity.LoyaltyPolicy;
import swp391.carwash.entity.LoyaltyTransaction;
import swp391.carwash.entity.MembershipTier;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.repository.LoyaltyAccountRepository;
import swp391.carwash.repository.LoyaltyPolicyRepository;
import swp391.carwash.repository.LoyaltyTransactionRepository;
import swp391.carwash.repository.MembershipTierRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerLoyaltyServiceImpl implements CustomerLoyaltyService {


    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final LoyaltyPolicyRepository loyaltyPolicyRepository;

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
}