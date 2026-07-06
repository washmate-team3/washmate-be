package swp391.carwash.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.*;
import swp391.carwash.enums.ComparisonOperator;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "insight_rule_config")
public class InsightRuleConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_config_id")
    private Integer id;

    @Column(name = "rule_code", nullable = false, unique = true, length = 100)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InsightType type;

    @Column(name = "threshold_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "comparison_operator", nullable = false, length = 40)
    private ComparisonOperator comparisonOperator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InsightSeverity severity;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String description;

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
