package swp391.carwash.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "insight_ai_enrichment")
public class InsightAIEnrichment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_insight_id", nullable = false, unique = true)
    private BusinessInsight businessInsight;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;

    @Column(name = "ai_recommendation", columnDefinition = "TEXT")
    private String aiRecommendation;

    @Column(name = "ai_campaign_suggestion", columnDefinition = "TEXT")
    private String aiCampaignSuggestion;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

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
