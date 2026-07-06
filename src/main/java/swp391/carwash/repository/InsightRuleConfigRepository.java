package swp391.carwash.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.InsightRuleConfig;

public interface InsightRuleConfigRepository extends JpaRepository<InsightRuleConfig, Integer> {
    List<InsightRuleConfig> findByActiveTrue();

    List<InsightRuleConfig> findByRuleCodeIn(Collection<String> ruleCodes);

    Optional<InsightRuleConfig> findByRuleCode(String ruleCode);
}
