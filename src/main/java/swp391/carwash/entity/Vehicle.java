package swp391.carwash.entity;

import jakarta.persistence.*;
import lombok.*;
import swp391.carwash.common.domain.AuditableEntity;
import swp391.carwash.enums.RecordStatus;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "vehicle")
public class Vehicle extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "license_plate", nullable = false, length = 30)
    private String licensePlate;

    private String brand;
    private String model;
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RecordStatus status = RecordStatus.ACTIVE;
}
