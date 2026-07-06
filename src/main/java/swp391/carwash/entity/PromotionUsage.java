package swp391.carwash.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "promotion_usage",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_promotion", columnNames = {"user_id", "promotion_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usage_promotion"))
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usage_user"))
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usage_booking"))
    private Booking booking;

    @Column(name = "used_at", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT NOW()")
    private OffsetDateTime usedAt;
}