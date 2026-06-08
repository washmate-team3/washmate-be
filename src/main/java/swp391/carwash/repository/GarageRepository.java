package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.Garage;

public interface GarageRepository extends JpaRepository<Garage, Integer> {
}
