package swp391.carwash.service;

import swp391.carwash.dto.request.Account.LoyaltyTierRequest;
import swp391.carwash.dto.response.vehicles.LoyaltyTierResponse;

import java.util.List;

public interface LoyaltyTierService {

    List<LoyaltyTierResponse> getAllTiers(Integer garageId);

    LoyaltyTierResponse createTier(Integer garageId, LoyaltyTierRequest request);

    LoyaltyTierResponse updateTier(Integer garageId, Integer tierId, LoyaltyTierRequest request);

    void deleteTier(Integer garageId, Integer tierId);

    LoyaltyTierResponse getById(Integer garageId, Integer id);
}
