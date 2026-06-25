package swp391.carwash.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.CustomerAiInsight;

public interface CustomerAiInsightRepository extends JpaRepository<CustomerAiInsight, Integer> {
    @EntityGraph(attributePaths = {"garage", "user"})
    List<CustomerAiInsight> findByUserIdOrderByGeneratedAtDesc(Integer userId);

    @EntityGraph(attributePaths = {"garage", "user"})
    List<CustomerAiInsight> findByGarageIdAndPeriodOrderByGeneratedAtDesc(Integer garageId, String period);

    Optional<CustomerAiInsight> findByUserIdAndGarageIdAndPeriodAndInsightTypeAndModelVersion(
            Integer userId,
            Integer garageId,
            String period,
            String insightType,
            String modelVersion);
}
