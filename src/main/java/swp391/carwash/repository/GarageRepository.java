package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import swp391.carwash.entity.Garage;
import java.util.List;

public interface GarageRepository extends JpaRepository<Garage, Integer> {
    // Tìm các garage đang hoạt động (phục vụ cho khách hàng xem danh sách)
    @Query("SELECT g FROM Garage g WHERE g.status <> 'DELETED'")
    List<Garage> findAllActiveGarages();

    boolean existsByName(String name);
}
