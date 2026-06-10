package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.entity.ServicePackage;
import swp391.carwash.service.ServicePackageService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ServicePackageController {

    private final ServicePackageService servicePackageService;


    // 1. API Tạo gói dịch vụ mới (Body cần truyền kèm garageId)
    // POST http://localhost:8080/api/v1/services
    @PostMapping
    public ResponseEntity<?> createService(@RequestBody ServicePackage servicePackage) {
        try {
            return ResponseEntity.ok(servicePackageService.createService(servicePackage));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Trả về lỗi nếu garageId không tồn tại
        }
    }

    // 2. API Lấy toàn bộ gói dịch vụ của MỘT Garage cụ thể (Dùng rất nhiều ở FE khi đổi chi nhánh)
    // GET http://localhost:8080/api/v1/services/garage/{garageId}
    @GetMapping("/garage/{garageId}")
    public ResponseEntity<List<ServicePackage>> getServicesByGarageId(@PathVariable Long garageId) {
        return ResponseEntity.ok(servicePackageService.getServicesByGarageId(garageId));
    }

    // 3. API Lấy thông tin chi tiết của 1 gói dịch vụ
    // GET http://localhost:8080/api/v1/services/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ServicePackage> getServiceById(@PathVariable Long id) {
        return servicePackageService.getServiceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. API Chỉnh sửa thông tin gói dịch vụ (Tên, giá tiền, thời gian, trạng thái)
    // PUT http://localhost:8080/api/v1/services/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id, @RequestBody ServicePackage serviceDetails) {
        try {
            return ResponseEntity.ok(servicePackageService.updateService(id, serviceDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 5. API Xóa gói dịch vụ
    // DELETE http://localhost:8080/api/v1/services/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        try {
            servicePackageService.deleteService(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
