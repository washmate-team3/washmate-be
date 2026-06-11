package swp391.carwash.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.LoyaltyTransaction;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Integer> {
    @EntityGraph(attributePaths = {"account", "account.garage", "booking", "sourceTransaction"})
    List<LoyaltyTransaction> findByAccountUserIdOrderByCreatedAtDesc(Integer userId);
}
