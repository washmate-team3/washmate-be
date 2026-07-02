package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.ServicePackage;
import swp391.carwash.enums.RecordStatus;
import java.util.List;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Integer> {
    // Tìm kiếm các gói dịch vụ phân theo chi nhánh garage cụ thể
    List<ServicePackage> findByGarageId(Integer garageId);
    List<ServicePackage> findByStatus(RecordStatus status);
}
