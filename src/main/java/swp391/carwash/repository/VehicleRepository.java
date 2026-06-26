package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.Vehicle;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    // tìm xe theo id chưa bị xóa
    Optional<Vehicle> findByIdAndDeletedAtIsNull(Integer vehicleId);

    // lấy danh sách xe của user
    List<Vehicle> findByUser_IdAndDeletedAtIsNull(Integer userId);

    List<Vehicle> findVehicleByUserId(Integer userId);

    // kiểm tra biển số tồn tại
    boolean existsByLicensePlateAndDeletedAtIsNull(String licensePlate);
    ;

}
