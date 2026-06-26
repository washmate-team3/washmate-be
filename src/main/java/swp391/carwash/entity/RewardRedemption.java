package swp391.carwash.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "reward_redemption", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "redemption_id")
    private Integer redemptionId;

    // Liên kết tới bảng loyalty_account qua cột account_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private LoyaltyAccount loyaltyAccount;

    // Liên kết tới bảng garage qua cột garage_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;

    // Liên kết tới bảng reward qua cột reward_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_id", nullable = false)
    private Reward reward;

    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, APPROVED, COMPLETED, REJECTED, CANCELLED

    @Column(name = "redeemed_at", nullable = false)
    private ZonedDateTime redeemedAt;

    @Column(name = "approved_at")
    private ZonedDateTime approvedAt;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;
}
