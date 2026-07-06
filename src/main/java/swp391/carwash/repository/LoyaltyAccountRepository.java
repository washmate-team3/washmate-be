package swp391.carwash.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.LoyaltyAccount;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Integer> {
    @EntityGraph(attributePaths = {"garage", "tier"})
    List<LoyaltyAccount> findByUserIdOrderByGarageNameAsc(Integer userId);

    Optional<LoyaltyAccount> findByUserIdAndGarageId(Integer userId, Integer garageId);

    Boolean existsByTierId(Integer tierId);

    Optional<LoyaltyAccount> findByUserId(Integer userId);
}
