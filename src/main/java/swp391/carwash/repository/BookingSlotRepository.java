package swp391.carwash.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.BookingSlot;
import swp391.carwash.enums.RecordStatus;

@Repository
public interface BookingSlotRepository extends JpaRepository<BookingSlot, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from BookingSlot slot where slot.id = :id")
    Optional<BookingSlot> findByIdForUpdate(@Param("id") Integer id);

}
