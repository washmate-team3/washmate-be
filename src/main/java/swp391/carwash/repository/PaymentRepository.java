package swp391.carwash.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.carwash.enums.PaymentMethod;
import swp391.carwash.enums.PaymentStatus;
import swp391.carwash.entity.Payment;
import java.time.OffsetDateTime;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    @EntityGraph(attributePaths = { "booking", "garage" })
    Optional<Payment> findByBookingId(Integer bookingId);

    @EntityGraph(attributePaths = { "booking", "booking.user", "booking.garage", "booking.slot", "booking.service",
            "booking.vehicle", "garage" })
    @Query("select payment from Payment payment where payment.id = :id")
    Optional<Payment> findDetailedById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "booking", "booking.user", "booking.garage", "booking.slot", "booking.service",
            "booking.vehicle", "garage" })
    @Query("select payment from Payment payment where payment.id = :id")
    Optional<Payment> findDetailedByIdForUpdate(@Param("id") Integer id);

    List<Payment> findByBookingIdIn(List<Integer> bookingIds);

    long countByStatus(PaymentStatus status);

    @Query("""
            select payment.id from Payment payment
            where payment.method = :method
              and payment.status = :status
              and payment.expiresAt is not null
              and payment.expiresAt <= :now
            """)
    List<Integer> findExpiredPaymentIds(
            @Param("method") PaymentMethod method,
            @Param("status") PaymentStatus status,
            @Param("now") OffsetDateTime now);
}
