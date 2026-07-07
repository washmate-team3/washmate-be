package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.request.Account.LoyaltyTierRequest;
import swp391.carwash.dto.response.vehicles.LoyaltyTierResponse;
import swp391.carwash.entity.MembershipTier;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.repository.GarageRepository;
import swp391.carwash.repository.LoyaltyAccountRepository;
import swp391.carwash.repository.MembershipTierRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltyTierServiceImpl implements LoyaltyTierService{


    private final MembershipTierRepository membershipTierRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final GarageRepository garageRepository;


    @Override
    @Transactional(readOnly = true)
    public List<LoyaltyTierResponse> getAllTiers(Integer garageId) {

        return membershipTierRepository
                .findByGarageIdAndStatusOrderByMinPointsAsc(
                        garageId,
                        RecordStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public LoyaltyTierResponse createTier(Integer garageId,
                                          LoyaltyTierRequest request) {

        validateCreate(garageId, request);

        MembershipTier tier = MembershipTier.builder()
                .id(garageId)
                .tierName(request.getTierName().trim())
                .minPoints(request.getMinPoints())
                .maintainPoints(request.getMaintainPoints())
                .discountPercentage(request.getDiscountPercentage())
                .status(RecordStatus.ACTIVE)
                .build();

        MembershipTier savedTier = membershipTierRepository.save(tier);

        return toResponse(savedTier);
    }

    @Override
    @Transactional
    public LoyaltyTierResponse updateTier(Integer garageId,
                                          Integer tierId,
                                          LoyaltyTierRequest request) {

        MembershipTier tier = getTier(tierId);

        if (!tier.getId().equals(garageId)) {
            throw new RuntimeException("Tier does not belong to this garage.");
        }

        validateUpdate(request, tier);

        tier.setTierName(request.getTierName().trim());
        tier.setMinPoints(request.getMinPoints());
        tier.setMaintainPoints(request.getMaintainPoints());
        tier.setDiscountPercentage(request.getDiscountPercentage());

        MembershipTier updatedTier = membershipTierRepository.save(tier);

        return toResponse(updatedTier);
    }

    @Override
    @Transactional
    public void deleteTier(Integer garageId, Integer tierId) {

        MembershipTier tier = getTier(tierId);

        if (!tier.getId().equals(garageId)) {
            throw new RuntimeException("Tier does not belong to this garage.");
        }

        if (loyaltyAccountRepository.existsByTierId(tierId)) {
            throw new RuntimeException("Tier is currently in use.");
        }

        tier.setStatus(RecordStatus.DELETED);

        membershipTierRepository.save(tier);
    }

    @Override
    public LoyaltyTierResponse getById(Integer garageId, Integer id) {
        return null;
    }

    private LoyaltyTierResponse toResponse(MembershipTier tier) {
        LoyaltyTierResponse response = new LoyaltyTierResponse();

        response.setTierId(tier.getId());
        response.setGarageId(tier.getGarage().getId());
        response.setTierName(tier.getTierName());
        response.setMinPoints(tier.getMinPoints());
        response.setMaintainPoints(tier.getMaintainPoints());
        response.setDiscountPercentage(tier.getDiscountPercentage());
        response.setStatus(tier.getStatus());

        return response;
    }
    private MembershipTier getTier(Integer tierId) {

        return membershipTierRepository.findById(tierId)
                .orElseThrow(() ->
                        new RuntimeException("Membership tier not found."));
    }

    private void validateCreate(Integer garageId,
                                LoyaltyTierRequest request) {

        if (membershipTierRepository
                .existsByGarageIdAndTierNameIgnoreCaseAndStatusNot(
                        garageId,
                        request.getTierName(),
                        RecordStatus.DELETED)) {

            throw new RuntimeException("Tier name already exists.");
        }

        validatePoint(request);
    }
    private void validateUpdate(LoyaltyTierRequest request,
                                MembershipTier tier) {

        Integer garageId = tier.getGarage().getId();

        if (membershipTierRepository
                .existsByGarageIdAndTierNameIgnoreCaseAndStatusNotAndIdNot(
                        garageId,
                        request.getTierName(),
                        RecordStatus.DELETED,
                        tier.getId())) {

            throw new RuntimeException("Tên hạng thành viên đã tồn tại.");
        }

        if (membershipTierRepository
                .existsByGarageIdAndMinPointsAndStatusNotAndIdNot(
                        garageId,
                        request.getMinPoints(),
                        RecordStatus.DELETED,
                        tier.getId())) {

            throw new RuntimeException("Mức điểm tối thiểu đã tồn tại.");
        }

        validatePoint(request);
    }

    private void validatePoint(LoyaltyTierRequest request) {

        if (request.getMinPoints() < 0) {
            throw new RuntimeException("Minimum points must be >= 0.");
        }

        if (request.getMaintainPoints() < 0) {
            throw new RuntimeException("Maintain points must be >= 0.");
        }

        if (request.getMaintainPoints() > request.getMinPoints()) {
            throw new RuntimeException("Maintain points cannot exceed minimum points.");
        }

        if (request.getDiscountPercentage().compareTo(BigDecimal.ZERO) < 0
                || request.getDiscountPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new RuntimeException("Discount percentage must be between 0 and 100.");
        }
    }
}
