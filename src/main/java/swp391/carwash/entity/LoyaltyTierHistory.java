package swp391.carwash.entity;

import jakarta.persistence.*;
import lombok.*;
import swp391.carwash.enums.TierChangeType;

import java.time.OffsetDateTime;

@Entity
@Table(name = "loyalty_tier_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTierHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private LoyaltyAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_tier_id")
    private MembershipTier oldTier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_tier_id", nullable = false)
    private MembershipTier newTier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private AppUser changedBy;

    @Column(name = "change_reason")
    private String changeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private TierChangeType changeType;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}