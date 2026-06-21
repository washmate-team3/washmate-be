package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.BookingSlot;
import java.util.List;

@Repository
public interface BookingSlotRepository extends JpaRepository<BookingSlot, Integer> {

    // Tìm toàn bộ khung giờ của một Garage cụ thể
    List<BookingSlot> findByGarageId(Integer garageId);

    // Tìm các khung giờ đang hoạt động (ACTIVE) của một Garage
    List<BookingSlot> findByGarageIdAndStatus(Integer garageId, String status);
}