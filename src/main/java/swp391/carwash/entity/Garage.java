package swp391.carwash.entity;

import jakarta.persistence.*;
import lombok.*;
import swp391.carwash.common.domain.AuditableEntity;
import swp391.carwash.enums.GarageStatus;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "garage")
public class Garage extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "garage_id")
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GarageStatus status = GarageStatus.ACTIVE;
}
