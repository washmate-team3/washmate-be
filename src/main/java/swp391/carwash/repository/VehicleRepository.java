package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
}
