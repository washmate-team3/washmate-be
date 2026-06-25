package swp391.carwash.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "customer_ai_insight",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_customer_ai_insight",
                columnNames = {"user_id", "garage_id", "period", "insight_type", "model_version"}
        )
)
public class CustomerAiInsight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insight_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(name = "insight_type", nullable = false, length = 50)
    private String insightType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prediction_value", nullable = false)
    private Map<String, Object> predictionValue;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "generated_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime generatedAt = OffsetDateTime.now();
}
