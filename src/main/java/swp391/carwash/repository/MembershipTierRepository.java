package swp391.carwash.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.MembershipTier;
import swp391.carwash.enums.RecordStatus;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier, Integer> {


    Optional<MembershipTier> findFirstByGarageIdAndStatusOrderByMinPointsAsc(
            Integer garageId,
            RecordStatus status
    );

    Optional<MembershipTier> findFirstByGarageIdAndStatusAndMinPointsLessThanEqualOrderByMinPointsDesc(
            Integer garageId,
            RecordStatus status,
            Integer minPoints
    );

    List<MembershipTier> findByGarageIdAndStatusOrderByMinPointsAsc(
            Integer garageId,
            RecordStatus status
    );

    boolean existsByGarageIdAndTierNameIgnoreCaseAndStatusNot(
            Integer garageId,
            String tierName,
            RecordStatus status
    );

    boolean existsByGarageIdAndTierNameIgnoreCaseAndStatusNotAndIdNot(
            Integer garageId,
            String tierName,
            RecordStatus status,
            Integer id
    );

    boolean existsByGarageIdAndMinPointsAndStatusNotAndIdNot(
            Integer garageId,
            Integer minPoints,
            RecordStatus status,
            Integer id
    );
    Optional<MembershipTier> findFirstByGarageIdAndStatusAndMinPointsLessThanOrderByMinPointsDesc(
            Integer garageId,
            RecordStatus status,
            Integer minPoints
    );
}
