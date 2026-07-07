package swp391.carwash.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.ServicePackage;
import swp391.carwash.enums.RecordStatus;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Integer> {
    List<ServicePackage> findByGarageId(Integer garageId);

    List<ServicePackage> findByStatus(RecordStatus status);

    @Query("""
            select service from ServicePackage service
            where service.status = :status
              and (:garageId is null or service.garage.id = :garageId)
            """)
    List<ServicePackage> findActiveForInsightScope(
            @Param("status") RecordStatus status,
            @Param("garageId") Integer garageId);
}
