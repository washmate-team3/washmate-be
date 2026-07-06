package swp391.carwash.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.carwash.entity.BusinessInsight;
import swp391.carwash.enums.InsightStatus;
import swp391.carwash.enums.InsightType;

public interface BusinessInsightRepository extends JpaRepository<BusinessInsight, Integer> {
    Optional<BusinessInsight> findByRuleCodeAndFromDateAndToDate(String ruleCode, LocalDate fromDate, LocalDate toDate);

    @Query("""
            select insight from BusinessInsight insight
            where insight.fromDate = :fromDate
              and insight.toDate = :toDate
              and (:type is null or insight.type = :type)
              and (:status is null or insight.status = :status)
            order by insight.severity asc, insight.createdAt desc
            """)
    List<BusinessInsight> findForOwnerInsights(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("type") InsightType type,
            @Param("status") InsightStatus status);
}
