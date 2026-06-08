package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
}
