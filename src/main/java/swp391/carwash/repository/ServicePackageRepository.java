package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.BookingSlot;
import swp391.carwash.entity.ServicePackage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {
    // Tìm kiếm các gói dịch vụ phân theo chi nhánh garage cụ thể
    List<ServicePackage> findByGarageId(Long garageId);
}