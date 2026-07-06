package swp391.carwash.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightStatus;
import swp391.carwash.enums.InsightType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "business_insight",
        uniqueConstraints = @UniqueConstraint(name = "uq_business_insight_rule_period", columnNames = {"rule_code", "from_date", "to_date"}))
public class BusinessInsight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insight_id")
    private Integer id;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InsightType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InsightSeverity severity;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String evidence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String meaning;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "related_metric", length = 100)
    private String relatedMetric;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InsightStatus status = InsightStatus.NEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
