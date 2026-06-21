package swp391.carwash.service;
import swp391.carwash.dto.request.Garages.CreateGarageRequest;
import swp391.carwash.dto.request.Garages.UpdateGarageRequest;
import swp391.carwash.dto.respone.Garages.GarageResponse;
import swp391.carwash.entity.Garage;

import swp391.carwash.entity.Garage;
import java.util.List;
import java.util.Optional;

public interface GarageService {

    // Tạo mới một chi nhánh Garage
    GarageResponse createGarage(CreateGarageRequest request);

    // Lấy danh sách tất cả Garage (Chỉ lấy các bản ghi chưa bị xóa mềm)
    List<Garage> getAllGarages();

    // Lấy thông tin chi tiết một Garage theo ID
    Optional<Garage> getGarageById(Integer id);

    // Cập nhật thông tin chi tiết một Garage
    GarageResponse updateGarage(Integer id, UpdateGarageRequest request);

    // Xóa một Garage (Sử dụng cơ chế Soft Delete)
    void deleteGarage(Integer id);
}
