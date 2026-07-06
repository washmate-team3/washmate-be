package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.request.Loyalty.LoyaltyPolicyRequest;
import swp391.carwash.dto.response.Loyaty.LoyaltyPolicyResponse;
import swp391.carwash.entity.Garage;
import swp391.carwash.entity.LoyaltyPolicy;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.repository.GarageRepository;
import swp391.carwash.repository.LoyaltyPolicyRepository;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LoyaltyPolicyServiceImpl implements LoyaltyPolicyService {

    private final LoyaltyPolicyRepository loyaltyPolicyRepository;
    private final GarageRepository garageRepository;

    @Override
    @Transactional(readOnly = true)
    public LoyaltyPolicyResponse getPolicy(Integer garageId) {

        LoyaltyPolicy policy = getPolicyEntity(garageId);

        return toResponse(policy);
    }

    @Override
    @Transactional
    public LoyaltyPolicyResponse create(Integer garageId,
                                        LoyaltyPolicyRequest request) {

        loyaltyPolicyRepository
                .findByGarageIdAndStatus(garageId, RecordStatus.ACTIVE)
                .ifPresent(policy -> {
                    throw new RuntimeException("Loyalty policy already exists.");
                });

        Garage garage = garageRepository.findById(garageId)
                .orElseThrow(() -> new RuntimeException("Garage not found."));

        LoyaltyPolicy policy = LoyaltyPolicy.builder()
                .garage(garage)
                .amountPerPoint(request.getAmountPerPoint())
                .pointExpiryMonths(request.getPointExpiryMonths())
                .autoEnroll(request.getAutoEnroll())
                .status(RecordStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        return toResponse(loyaltyPolicyRepository.save(policy));
    }

    @Override
    @Transactional
    public LoyaltyPolicyResponse update(Integer garageId,
                                        LoyaltyPolicyRequest request) {

        LoyaltyPolicy policy = getPolicyEntity(garageId);

        policy.setAmountPerPoint(request.getAmountPerPoint());
        policy.setPointExpiryMonths(request.getPointExpiryMonths());
        policy.setAutoEnroll(request.getAutoEnroll());
        policy.setUpdatedAt(OffsetDateTime.now());

        return toResponse(loyaltyPolicyRepository.save(policy));
    }

    @Override
    @Transactional
    public void delete(Integer garageId) {

        LoyaltyPolicy policy = getPolicyEntity(garageId);

        policy.setStatus(RecordStatus.DELETED);
        policy.setUpdatedAt(OffsetDateTime.now());

        loyaltyPolicyRepository.save(policy);
    }
    private LoyaltyPolicy getPolicyEntity(Integer garageId) {

        return loyaltyPolicyRepository
                .findByGarageIdAndStatus(
                        garageId,
                        RecordStatus.ACTIVE)
                .orElseThrow(() ->
                        new RuntimeException("Loyalty policy not found."));
    }
    private LoyaltyPolicyResponse toResponse(LoyaltyPolicy policy) {

        return LoyaltyPolicyResponse.builder()
                .policyId(policy.getId())
                .garageId(policy.getGarage().getId())
                .amountPerPoint(policy.getAmountPerPoint())
                .pointExpiryMonths(policy.getPointExpiryMonths())
                .autoEnroll(policy.getAutoEnroll())
                .build();
    }
}