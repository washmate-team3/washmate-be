package swp391.carwash.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.Reward;
import swp391.carwash.enums.RewardStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Integer> {

    Page<Reward> findByGarageIdAndStatusAndStockGreaterThan(
            Integer garageId,
            String status,
            Integer stock,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM Reward r
            JOIN FETCH r.garage
            WHERE r.rewardId = :rewardId
            """)
    Optional<Reward> findByIdForUpdate(
            @Param("rewardId") Integer rewardId
    );

    Page<Reward> findByGarageIdAndStatus(
            Integer garageId,
            RewardStatus status,
            Pageable pageable
    );

    Page<Reward> findByGarageIdAndStatusNot(
            Integer garageId,
            RewardStatus status,
            Pageable pageable
    );

}
