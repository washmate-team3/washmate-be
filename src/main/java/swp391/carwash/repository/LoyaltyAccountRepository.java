package swp391.carwash.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.LoyaltyAccount;
import swp391.carwash.enums.RecordStatus;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Integer> {
    @EntityGraph(attributePaths = {"garage", "tier"})
    List<LoyaltyAccount> findByUserIdOrderByGarageNameAsc(Integer userId);

    Optional<LoyaltyAccount> findByUserIdAndGarageId(Integer userId, Integer garageId);

    Boolean existsByTierId(Integer tierId);

    Optional<LoyaltyAccount> findByUserId(Integer userId);
    @Query("""
            select count(account)
            from LoyaltyAccount account
            where account.status = :status
              and account.availablePoints > 0
            """)
    long countAccountsWithAvailablePoints(@Param("status") RecordStatus status);

    @Query("""
            select count(account)
            from LoyaltyAccount account
            where account.status = :status
              and account.availablePoints > 0
              and (:garageId is null or account.garage.id = :garageId)
            """)
    long countAccountsWithAvailablePointsForScope(
            @Param("status") RecordStatus status,
            @Param("garageId") Integer garageId);

    List<LoyaltyAccount> findByStatusAndTierStatus(
            RecordStatus accountStatus,
            RecordStatus tierStatus
    );

    @EntityGraph(attributePaths = {
            "tier",
            "garage",
            "user"
    })
    List<LoyaltyAccount> findByStatus(RecordStatus status);
}
