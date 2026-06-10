package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.Garage;

import java.util.List;

public interface GarageRepository extends JpaRepository<Garage,Integer> {
    // Tìm các garage đang hoạt động (phục vụ cho khách hàng xem danh sách)
    List<Garage> findByStatus(String status);

    // Kiểm tra xem số điện thoại của garage đã tồn tại chưa khi tạo mới
    boolean existsByPhone(String phone);

}
