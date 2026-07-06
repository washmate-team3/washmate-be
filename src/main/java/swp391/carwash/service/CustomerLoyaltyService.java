package swp391.carwash.service;

import java.util.List;
import swp391.carwash.dto.LoyaltyAccountResponse;
import swp391.carwash.dto.LoyaltyTransactionResponse;
import swp391.carwash.dto.response.Loyaty.LoyaltyPolicyResponse;
import swp391.carwash.dto.response.vehicles.LoyaltyTierResponse;

public interface CustomerLoyaltyService {

    LoyaltyAccountResponse getMyLoyalty(Integer userId);

    List<LoyaltyTierResponse> getTiers(Integer garageId);

    List<LoyaltyTransactionResponse> getTransactions(Integer userId);

    LoyaltyPolicyResponse getPolicy(Integer garageId);
}
