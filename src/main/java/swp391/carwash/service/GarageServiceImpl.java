package swp391.carwash.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.request.Garages.CreateGarageRequest;
import swp391.carwash.dto.request.Garages.UpdateGarageRequest;
import swp391.carwash.dto.respone.Garages.GarageResponse;
import swp391.carwash.entity.Garage;
import swp391.carwash.repository.GarageRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GarageServiceImpl implements GarageService {

    private final GarageRepository garageRepository;

    // Sử dụng Constructor Injection thay vì @Autowired theo chuẩn khuyến nghị
    public GarageServiceImpl(GarageRepository garageRepository) {
        this.garageRepository = garageRepository;
    }

    @Override
    @Transactional
    public GarageResponse createGarage(CreateGarageRequest request) {
        // Kiểm tra trùng tên dưới DB
        if (garageRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Tên Garage này đã tồn tại trên hệ thống!");
        }

        // Tạo thực thể Entity từ DTO Request truyền từ Controller sang
        Garage garage = Garage.builder()
                .name(request.name())
                .address(request.address())
                .phone(request.phone())
                .status(swp391.carwash.enums.GarageStatus.ACTIVE)
                .build();

        // Lưu xuống DB
        Garage savedGarage = garageRepository.save(garage);

        // Map ngược lại sang DTO Response để trả về cho Controller
        return new GarageResponse(
                savedGarage.getId() != null ? savedGarage.getId().intValue() : null,
                savedGarage.getName(),
                savedGarage.getAddress(),
                savedGarage.getPhone(),
                savedGarage.getStatus().name(),
                savedGarage.getCreatedAt() != null ? savedGarage.getCreatedAt().toLocalDateTime() : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Garage> getAllGarages() {
        // Gọi hàm từ Repository để loại trừ hoàn toàn các chi nhánh đã đánh dấu 'DELETED'
        return garageRepository.findAllActiveGarages();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Garage> getGarageById(Integer id) {
        return garageRepository.findById(id)
                .filter(garage -> garage.getStatus() != swp391.carwash.enums.GarageStatus.DELETED);
    }

    @Override
    @Transactional
    public GarageResponse updateGarage(Integer id, UpdateGarageRequest request) {
        // 1. Tìm Garage cũ theo ID, lọc bỏ các chi nhánh đã bị xóa mềm (DELETED)
        Garage existingGarage = garageRepository.findById(id)
                .filter(garage -> garage.getStatus() != swp391.carwash.enums.GarageStatus.DELETED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Garage với ID: " + id));

        // 2. Cập nhật thông tin mới từ DTO Request vào thực thể Entity cũ dưới DB
        existingGarage.setName(request.name());
        existingGarage.setAddress(request.address());
        existingGarage.setPhone(request.phone());
        existingGarage.setStatus(swp391.carwash.enums.GarageStatus.valueOf(request.status()));

        // 3. Sử dụng OffsetDateTime.now() ghi nhận thời gian update khớp với múi giờ PostgreSQL timestamptz
        existingGarage.setUpdatedAt(OffsetDateTime.now());

        // 4. Lưu thực thể đã cập nhật xuống database
        Garage updatedGarage = garageRepository.save(existingGarage);

        // 5. Map ngược lại sang DTO Response giống hệt cấu trúc hàm create của ông
        return new GarageResponse(
                updatedGarage.getId() != null ? updatedGarage.getId().intValue() : null,
                updatedGarage.getName(),
                updatedGarage.getAddress(),
                updatedGarage.getPhone(),
                updatedGarage.getStatus().name(),
                updatedGarage.getCreatedAt() != null ? updatedGarage.getCreatedAt().toLocalDateTime() : null
        );
    }

    @Override
    @Transactional
    public void deleteGarage(Integer id) {
        // Tìm kiếm thực thể trước khi thực hiện xóa mềm
        Garage existingGarage = garageRepository.findById(id)
                .filter(garage -> garage.getStatus() != swp391.carwash.enums.GarageStatus.DELETED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Garage với ID: " + id));

        // Thực hiện Soft Delete nghiệp vụ: Đổi trạng thái sang DELETED và lưu vết thời gian xóa
        existingGarage.setStatus(swp391.carwash.enums.GarageStatus.DELETED);
        existingGarage.setDeletedAt(OffsetDateTime.now());

        garageRepository.save(existingGarage);
    }
}
