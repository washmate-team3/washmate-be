package swp391.carwash.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import swp391.carwash.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    @EntityGraph(attributePaths = {"booking", "payment", "garage"})
    Optional<Invoice> findByBookingId(Integer bookingId);

    @EntityGraph(attributePaths = {"booking", "booking.user", "booking.garage", "booking.slot", "booking.service", "booking.vehicle", "payment", "garage"})
    @Query("select invoice from Invoice invoice where invoice.id = :id")
    Optional<Invoice> findDetailedById(Integer id);

    boolean existsByBookingId(Integer bookingId);

    List<Invoice> findByBookingIdIn(List<Integer> bookingIds);
}
