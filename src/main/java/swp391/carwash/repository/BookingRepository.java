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
        @EntityGraph(attributePaths = { "user", "garage", "slot", "service", "vehicle", "assignedStaff" })
        @Query("select b from Booking b where b.id = :id")
        Optional<Booking> findDetailedById(@Param("id") Integer id);

        @EntityGraph(attributePaths = { "user", "garage", "slot", "service", "vehicle", "assignedStaff" })
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

        @EntityGraph(attributePaths = { "user", "garage", "slot", "service", "vehicle", "assignedStaff" })
        @Query("""
                        select b from Booking b
                        where (cast(:status as text) is null or b.status = :status)
                          and (cast(:garageId as int) is null or b.garage.id = :garageId)
                          and (cast(:fromDate as date) is null or b.bookingDate >= :fromDate)
                          and (cast(:toDate as date) is null or b.bookingDate <= :toDate)
                          and (coalesce(:garageIds, null) is null or b.garage.id in :garageIds)
                        """)
        org.springframework.data.domain.Page<Booking> findBookingsWithFilters(
                        @Param("status") BookingStatus status,
                        @Param("garageId") Integer garageId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("garageIds") List<Integer> garageIds,
                        org.springframework.data.domain.Pageable pageable);

        @Query("""
                        select count(b)
                        from Booking b
                        where b.slot.id = :slotId
                          and b.garage.id = :garageId
                          and b.bookingDate = :bookingDate
                          and b.status in :statuses
                          and b.id != :excludeBookingId
                        """)
        long countActiveBookingsForUpdate(
                        @Param("slotId") Integer slotId,
                        @Param("garageId") Integer garageId,
                        @Param("bookingDate") LocalDate bookingDate,
                        @Param("statuses") Collection<BookingStatus> statuses,
                        @Param("excludeBookingId") Integer excludeBookingId);

        @Query("""
                        SELECT b.slot.id, count(b.id)
                        FROM Booking b
                        WHERE b.garage.id = :garageId
                          AND b.bookingDate = :bookingDate
                          AND b.status IN :statuses
                        GROUP BY b.slot.id
                        """)
        List<Object[]> countActiveBookingsGroupBySlot(
                        @Param("garageId") Integer garageId,
                        @Param("bookingDate") LocalDate bookingDate,
                        @Param("statuses") Collection<BookingStatus> statuses);
}
