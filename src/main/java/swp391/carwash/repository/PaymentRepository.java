package swp391.carwash.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import swp391.carwash.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    @EntityGraph(attributePaths = { "booking", "garage" })
    Optional<Payment> findByBookingId(Integer bookingId);

    @EntityGraph(attributePaths = { "booking", "booking.user", "booking.garage", "booking.slot", "booking.service",
            "booking.vehicle", "garage" })
    @Query("select payment from Payment payment where payment.id = :id")
    Optional<Payment> findDetailedById(Integer id);

    List<Payment> findByBookingIdIn(List<Integer> bookingIds);
}
