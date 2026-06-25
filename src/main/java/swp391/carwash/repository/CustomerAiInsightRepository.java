package swp391.carwash.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.CustomerAiInsight;

public interface CustomerAiInsightRepository extends JpaRepository<CustomerAiInsight, Integer> {
    @EntityGraph(attributePaths = {"garage"})
    List<CustomerAiInsight> findByUserIdOrderByGeneratedAtDesc(Integer userId);
}
