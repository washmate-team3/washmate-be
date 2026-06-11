package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.ServicePackage;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, Integer> {
}
