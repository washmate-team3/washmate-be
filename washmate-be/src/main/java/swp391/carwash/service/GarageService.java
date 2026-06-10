package swp391.carwash.service;

import swp391.carwash.entity.Garage;

import java.util.List;
import java.util.Optional;

public interface GarageService {
    // 1. Tạo mới một cơ sở Garage
    Garage createGarage(Garage garage);

    // 2. Xem danh sách tất cả các Garage (Cho khách hàng chọn cơ sở trên FE)
    List<Garage> getAllGarages();

    // 3. Lấy thông tin chi tiết của một Garage theo ID
    Optional<Garage> getGarageById(Long id);

    // 4. Cập nhật thông tin Garage (Tên, địa chỉ, trạng thái ACTIVE/INACTIVE)
    Garage updateGarage(Long id, Garage garageDetails);

    // 5. Xóa Garage khỏi hệ thống (Hoặc chuyển trạng thái sang INACTIVE)
    void deleteGarage(Long id);
}
