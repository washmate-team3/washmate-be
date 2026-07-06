package swp391.carwash.service;

import swp391.carwash.dto.request.Loyalty.LoyaltyPolicyRequest;
import swp391.carwash.dto.response.Loyaty.LoyaltyPolicyResponse;

public interface LoyaltyPolicyService {

    LoyaltyPolicyResponse getPolicy(Integer garageId);

    LoyaltyPolicyResponse create(Integer garageId,
                                 LoyaltyPolicyRequest request);

    LoyaltyPolicyResponse update(Integer garageId,
                                 LoyaltyPolicyRequest request);

    void delete(Integer garageId);
}
