package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.BookingSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;


public interface ServicePackageRepository extends JpaRepository<BookingSlot,Integer> {

    List<BookingSlot> findByGarage_Id(Integer garageId);

    // Tìm slot trùng giờ tại một Gara để tránh tạo trùng ca trùng ngày
    Optional<BookingSlot> findByGarageIdAndStartTimeAndEndTime(
            Integer garageId, LocalTime startTime, LocalTime endTime);
}
