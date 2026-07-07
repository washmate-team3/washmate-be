package swp391.carwash.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;

/**
 * Audit record for one AI deep-analysis run.
 * total_returned vs total_kept is the proof that hallucinated insights were filtered out.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "insight_analysis_run")
public class InsightAnalysisRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_run_id")
    private Integer id;

    @Column(name = "garage_id")
    private Integer garageId;

    @Column(name = "requested_by", nullable = false)
    private Integer requestedBy;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "ai_model", nullable = false, length = 100)
    private String aiModel;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "total_returned", nullable = false)
    @Builder.Default
    private Integer totalReturned = 0;

    @Column(name = "total_kept", nullable = false)
    @Builder.Default
    private Integer totalKept = 0;

    @Column(name = "total_rejected", nullable = false)
    @Builder.Default
    private Integer totalRejected = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
