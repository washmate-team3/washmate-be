package swp391.carwash.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Table(name = "app_user")
public class AppUser {

    @Id
    @Column(name = "user_id")
    private Integer id;

    private String email;

    private String passwordHash;

    private String fullName;

    private String phone;

    private String status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;

}