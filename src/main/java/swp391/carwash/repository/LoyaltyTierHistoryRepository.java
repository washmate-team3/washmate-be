package swp391.carwash.repository;

import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.carwash.entity.LoyaltyTierHistory;
import swp391.carwash.enums.TierChangeType;

public interface LoyaltyTierHistoryRepository extends JpaRepository<LoyaltyTierHistory,Integer> {

    @Query("""
            select count(history) from LoyaltyTierHistory history
            where history.changeType = :changeType
              and history.createdAt >= :fromTime
              and history.createdAt < :toTime
              and (:garageId is null or history.garage.id = :garageId)
            """)
    long countByChangeTypeInPeriod(
            @Param("changeType") TierChangeType changeType,
            @Param("fromTime") OffsetDateTime fromTime,
            @Param("toTime") OffsetDateTime toTime,
            @Param("garageId") Integer garageId);
}
