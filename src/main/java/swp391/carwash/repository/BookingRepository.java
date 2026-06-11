package swp391.carwash.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.carwash.entity.Booking;
import swp391.carwash.enums.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    @EntityGraph(attributePaths = {"user", "garage", "slot", "service", "vehicle", "assignedStaff"})
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findDetailedById(@Param("id") Integer id);

    @EntityGraph(attributePaths = {"user", "garage", "slot", "service", "vehicle", "assignedStaff"})
    List<Booking> findByUserIdOrderByCreatedAtDesc(Integer userId);

    @Query("""
            select count(b)
            from Booking b
            where b.slot.id = :slotId
              and b.garage.id = :garageId
              and b.bookingDate = :bookingDate
              and b.status in :statuses
            """)
    long countActiveBookings(
            @Param("slotId") Integer slotId,
            @Param("garageId") Integer garageId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("statuses") Collection<BookingStatus> statuses);
}
