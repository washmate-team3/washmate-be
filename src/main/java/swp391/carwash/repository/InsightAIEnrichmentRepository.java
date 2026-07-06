package swp391.carwash.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.InsightAIEnrichment;

@Repository
public interface InsightAIEnrichmentRepository extends JpaRepository<InsightAIEnrichment, Integer> {
    Optional<InsightAIEnrichment> findByBusinessInsightId(Integer businessInsightId);
    boolean existsByBusinessInsightId(Integer businessInsightId);
}
