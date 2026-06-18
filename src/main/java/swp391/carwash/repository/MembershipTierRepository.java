package swp391.carwash.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.MembershipTier;
import swp391.carwash.enums.RecordStatus;

public interface MembershipTierRepository extends JpaRepository<MembershipTier, Integer> {
    Optional<MembershipTier> findFirstByOrderByMinPointsAsc();

    Optional<MembershipTier> findFirstByGarageIdAndStatusOrderByMinPointsAsc(Integer garageId, RecordStatus status);

    Optional<MembershipTier> findFirstByGarageIdAndStatusAndMinPointsLessThanEqualOrderByMinPointsDesc(
            Integer garageId,
            RecordStatus status,
            Integer minPoints);
}
