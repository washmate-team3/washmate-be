package swp391.carwash.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import swp391.carwash.enums.TransactionType;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "earned_at", nullable = false)
    private OffsetDateTime earnedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expired", nullable = false)
    @Builder.Default
    private Boolean expired = false;
}
