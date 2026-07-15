package swp391.carwash.repository;

import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.CampaignSendLog;

public interface CampaignSendLogRepository extends JpaRepository<CampaignSendLog, Integer> {

    /** Chống gửi trùng: đã có lần gửi cho insight này sau mốc cutoff chưa. */
    boolean existsByInsightIdAndSentAtAfter(Integer insightId, OffsetDateTime cutoff);
}
