package swp391.carwash.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.Role;
import swp391.carwash.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(RoleName roleName);
}
