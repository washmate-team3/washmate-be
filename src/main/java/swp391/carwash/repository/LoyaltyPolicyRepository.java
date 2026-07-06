package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.LoyaltyPolicy;
import swp391.carwash.enums.RecordStatus;

import java.util.Optional;

public interface LoyaltyPolicyRepository
        extends JpaRepository<LoyaltyPolicy,Integer> {

    Optional<LoyaltyPolicy> findByGarageIdAndStatus(
            Integer garageId,
            RecordStatus status);

}