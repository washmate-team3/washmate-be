package swp391.carwash.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.AppUser;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userRoles.garage"})
    Optional<AppUser> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userRoles.garage"})
    Optional<AppUser> findByPhone(String phone);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userRoles.garage"})
    Optional<AppUser> findWithUserRolesById(Integer id);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userRoles.garage"})
    org.springframework.data.domain.Page<AppUser> findAll(org.springframework.data.domain.Pageable pageable);
}
