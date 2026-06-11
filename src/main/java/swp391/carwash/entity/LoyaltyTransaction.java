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
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "loyalty_transaction")
public class LoyaltyTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private LoyaltyAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "redemption_id")
    private Integer redemptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_transaction_id")
    private LoyaltyTransaction sourceTransaction;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "earned_at", nullable = false)
    private OffsetDateTime earnedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
