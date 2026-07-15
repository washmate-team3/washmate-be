package swp391.carwash.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nhật ký mỗi lần gửi chiến dịch từ insight: dùng để giải trình và chống gửi trùng.
 */
@Entity
@Table(name = "campaign_send_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer id;

    @Column(name = "insight_id", nullable = false)
    private Integer insightId;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "garage_id")
    private Integer garageId;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "sent_count", nullable = false)
    private Integer sentCount;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount;

    @Column(name = "sent_by_user_id")
    private Integer sentByUserId;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;
}
