package swp391.carwash.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.Vehicle;
import swp391.carwash.enums.RecordStatus;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    List<Vehicle> findByUserIdAndStatusOrderByIdDesc(Integer userId, RecordStatus status);
}
