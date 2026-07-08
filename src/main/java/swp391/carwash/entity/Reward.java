package swp391.carwash.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reward")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_id")
    private Integer rewardId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "points_required", nullable = false)
    private Integer pointsRequired;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private String status;
}