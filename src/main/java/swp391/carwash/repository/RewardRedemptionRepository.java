package swp391.carwash.repository;

import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.carwash.entity.RewardRedemption;

public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption,Integer> {
    // Admin/Staff xem toàn bộ đơn đổi quà của một Garage
    Page<RewardRedemption> findByGarage_Id(Integer garageId, Pageable pageable);

    // Admin/Staff xem đơn đổi quà của một Garage và lọc theo trạng thái (PENDING, APPROVED, COMPLETED, REJECTED)
    Page<RewardRedemption> findByGarage_IdAndStatus(Integer garageId, String status, Pageable pageable);

    // Customer tự xem lịch sử đổi quà cá nhân tại một Garage cụ thể (quét qua quan hệ nested: loyaltyAccount -> appUser -> userId)
    Page<RewardRedemption> findByLoyaltyAccount_User_IdAndGarage_Id(Integer userId, Integer garageId, Pageable pageable);
    @EntityGraph(attributePaths = {"loyaltyAccount", "loyaltyAccount.user", "garage", "reward"})
    @Query("""
            select redemption from RewardRedemption redemption
            where redemption.redeemedAt >= :fromTime
              and redemption.redeemedAt < :toTime
              and (:garageId is null or redemption.garage.id = :garageId)
            """)
    List<RewardRedemption> findForInsightPeriod(
            @Param("fromTime") ZonedDateTime fromTime,
            @Param("toTime") ZonedDateTime toTime,
            @Param("garageId") Integer garageId);

    default List<RewardRedemption> findForInsightPeriod(ZonedDateTime fromTime, ZonedDateTime toTime) {
        return findForInsightPeriod(fromTime, toTime, null);
    }
}
