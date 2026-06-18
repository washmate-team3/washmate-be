package swp391.carwash.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
