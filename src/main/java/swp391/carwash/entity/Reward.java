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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "points_required", nullable = false)
    private Integer pointsRequired;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "status") // nếu tên cột database khác tên biến
    private String status;

}