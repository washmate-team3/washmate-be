package swp391.carwash.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.OtpCode;
import swp391.carwash.enums.OtpChannel;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Integer> {
    Optional<OtpCode> findByChannelAndIdentifier(OtpChannel channel, String identifier);
    void deleteByChannelAndIdentifier(OtpChannel channel, String identifier);

    Optional<OtpCode> findByPhone(String phone);
    void deleteByPhone(String phone);
}
