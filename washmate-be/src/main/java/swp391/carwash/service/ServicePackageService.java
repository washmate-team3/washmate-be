package swp391.carwash.service;

import swp391.carwash.entity.ServicePackage;

import java.util.List;
import java.util.Optional;

public interface ServicePackageService {
    // 1. Tạo mới gói dịch vụ (Bắt buộc check xem garageId truyền vào có tồn tại không)
    ServicePackage createService(ServicePackage servicePackage);

    // 2. Lấy danh sách tất cả gói dịch vụ của MỘT Garage cụ thể (Để FE hiển thị theo chi nhánh)
    List<ServicePackage> getServicesByGarageId(Long garageId);

    // 3. Lấy chi tiết một gói dịch vụ theo ID
    Optional<ServicePackage> getServiceById(Long id);

    // 4. Cập nhật gói dịch vụ (Tên, Giá tiền Price, Thời gian Duration, Trạng thái)
    ServicePackage updateService(Long id, ServicePackage serviceDetails);

    // 5. Thay đổi trạng thái ẩn/hiện gói dịch vụ (ACTIVE/INACTIVE) thay vì xóa cứng
    void updateServiceStatus(Long id, String status);

    // 6. Xóa gói dịch vụ
    void deleteService(Long id);
}
