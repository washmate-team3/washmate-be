package swp391.carwash.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.request.ServicePackageRequest.CreateServicePackageRequest;
import swp391.carwash.dto.request.ServicePackageRequest.UpdateServicePackageRequest;
import swp391.carwash.dto.response.ServicePackage.ServicePackageResponse;
import swp391.carwash.entity.ServicePackage;
import swp391.carwash.repository.ServicePackageRepository;
import swp391.carwash.repository.GarageRepository;
import swp391.carwash.entity.Garage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicePackageServiceImpl implements ServicePackageService {

    private final ServicePackageRepository servicePackageRepository;
    private final GarageRepository garageRepository;

    @Override
    @Transactional
    public ServicePackageResponse createService(CreateServicePackageRequest request) {
        Garage garage = garageRepository.findById(request.getGarageId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Garage với ID: " + request.getGarageId()));

        // 1. Tạo Entity từ Request DTO thông qua Builder pattern
        ServicePackage servicePackage = ServicePackage.builder()
                .garage(garage)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .duration(request.getDurationMinutes())
                .status(swp391.carwash.enums.RecordStatus.ACTIVE)
                .build();

        // 2. Lưu thực thể xuống Database
        ServicePackage saved = servicePackageRepository.save(servicePackage);

        // 3. Chuyển đổi và trả về Response DTO sạch cho Controller
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicePackageResponse> getServicesByGarageId(Long garageId) {
        List<ServicePackage> services = servicePackageRepository.findByGarageId(garageId.intValue());
        return services.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ServicePackageResponse getServiceById(Long id) {
        ServicePackage servicePackage = servicePackageRepository.findById(id.intValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói dịch vụ với ID: " + id));
        return mapToResponse(servicePackage);
    }

    @Override
    @Transactional
    public ServicePackageResponse updateService(Long id, UpdateServicePackageRequest request) {
        ServicePackage servicePackage = servicePackageRepository.findById(id.intValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói dịch vụ với ID: " + id));

        // Tiến hành cập nhật thông tin mới từ Request DTO vào Entity
        servicePackage.setName(request.getName());
        servicePackage.setDescription(request.getDescription());
        servicePackage.setPrice(request.getPrice());
        servicePackage.setDuration(request.getDurationMinutes());
        servicePackage.setStatus(swp391.carwash.enums.RecordStatus.valueOf(request.getStatus()));


        ServicePackage updated = servicePackageRepository.save(servicePackage);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteService(Long id) {
        ServicePackage servicePackage = servicePackageRepository.findById(id.intValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói dịch vụ với ID: " + id));

        // Thực hiện xóa cứng bản ghi (Hoặc bạn có thể đổi thành soft delete nếu cần)
        servicePackageRepository.delete(servicePackage);
    }

    // Hàm Mapper nội bộ biến thực thể Entity thành Response DTO an toàn
    private ServicePackageResponse mapToResponse(ServicePackage entity) {
        return ServicePackageResponse.builder()
                .servicePackageId(entity.getId())
                .garageId(entity.getGarage().getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .durationMinutes(entity.getDuration())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}