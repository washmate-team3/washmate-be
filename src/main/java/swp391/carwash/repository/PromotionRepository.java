package swp391.carwash.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.carwash.entity.Promotion;
import swp391.carwash.enums.PromotionStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface    PromotionRepository
        extends JpaRepository<Promotion, Integer> {

    @Query("""
            SELECT DISTINCT p
            FROM RewardRedemption rr
            JOIN rr.promotion p
            JOIN rr.loyaltyAccount account
            WHERE p.garageId = :garageId
              AND account.user.id = :userId
              AND rr.status = 'COMPLETED'
              AND rr.booking IS NULL
              AND rr.usedAt IS NULL
              AND p.status = 'ACTIVE'
              AND :now BETWEEN p.startDate AND p.endDate
              AND (
                    p.usageLimit IS NULL
                    OR p.usedCount < p.usageLimit
              )
              AND NOT EXISTS (
                    SELECT pu.id
                    FROM PromotionUsage pu
                    WHERE pu.promotion = p
                      AND pu.user.id = :userId
              )
            ORDER BY p.discountValue DESC
            """)
    List<Promotion> findAvailablePromotions(
            @Param("garageId") Integer garageId,
            @Param("userId") Integer userId,
            @Param("now") OffsetDateTime now
    );

    List<Promotion> findByGarageId(
            Integer garageId
    );

    boolean existsByPromoCode(
            String promoCode
    );
    boolean existsByGarageIdAndPromoCodeIgnoreCase(
            Integer garageId,
            String promoCode
    );

    boolean existsByGarageIdAndPromoCodeIgnoreCaseAndPromotionIdNot(
            Integer garageId,
            String promoCode,
            Integer id
    );

    @Query("""
    SELECT p
    FROM Promotion p
    WHERE p.garageId = :garageId
      AND p.status <> :status
""")
    Page<Promotion> findByGarageIdAndStatusNot(
            Integer garageId,
            PromotionStatus status,
            Pageable pageable
    );

    Page<Promotion> findByStatusNot(
            PromotionStatus status,
            Pageable pageable
    );
}