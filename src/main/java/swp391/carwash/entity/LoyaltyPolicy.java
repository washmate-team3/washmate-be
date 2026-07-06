package swp391.carwash.entity;

import jakarta.persistence.*;
import lombok.*;
import swp391.carwash.enums.RecordStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "loyalty_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garage_id", nullable = false, unique = true)
    private Garage garage;

    @Column(name = "amount_per_point", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPerPoint;

    @Column(name = "point_expiry_months", nullable = false)
    private Integer pointExpiryMonths;

    @Column(name = "auto_enroll", nullable = false)
    private Boolean autoEnroll;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RecordStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}