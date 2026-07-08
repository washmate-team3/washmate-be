package swp391.carwash.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.Reward;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Integer> {

    Optional<Reward> findByRewardIdAndGarageId(
            Integer rewardId,
            Integer garageId
    );

    Page<Reward> findByGarageIdAndPromotionIsNotNullAndStatus(
            Integer garageId,
            String status,
            Pageable pageable
    );

    Page<Reward> findByGarageIdAndPromotionIsNotNullAndStatusNot(
            Integer garageId,
            String status,
            Pageable pageable
    );
}
