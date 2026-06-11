package swp391.carwash.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.LoyaltyAccount;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Integer> {
    @EntityGraph(attributePaths = {"garage", "tier"})
    List<LoyaltyAccount> findByUserIdOrderByGarageNameAsc(Integer userId);
}
