package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.carwash.dto.request.vehicles.LoyaltyTierRequest;
import swp391.carwash.dto.response.vehicles.LoyaltyTierResponse;
import swp391.carwash.repository.MembershipTierRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltyTierService {
    
    private final MembershipTierRepository membershipTierRepository;

    public List<LoyaltyTierResponse> getAllTiers() {
        return List.of();
    }

    public LoyaltyTierResponse createTier(LoyaltyTierRequest request) {
        return new LoyaltyTierResponse();
    }

    public LoyaltyTierResponse updateTier(Integer id, LoyaltyTierRequest request) {
        return new LoyaltyTierResponse();
    }

    public void deleteTier(Integer id) {
        // deletion logic
    }
}
