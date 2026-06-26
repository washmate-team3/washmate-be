package swp391.carwash.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.request.vehicles.CreateVehicleRequest;
import swp391.carwash.dto.request.vehicles.UpdateVehicleRequest;
import swp391.carwash.dto.response.vehicles.VehicleResponse;

import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.Vehicle;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.VehicleRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService{

    private final VehicleRepository vehicleRepository;
    @Autowired
    private AppUserRepository appUserRepository;


    @Override
    public VehicleResponse create(CreateVehicleRequest request) {

        if (!appUserRepository.existsById(request.getUserId())) {
            throw new RuntimeException("Người dùng với ID " + request.getUserId() + " không tồn tại trên hệ thống!");
        }

        if(vehicleRepository.existsByLicensePlateAndDeletedAtIsNull(request.getLicensePlate()))
            throw new RuntimeException("biển số xe này đã tồn tại");

        Vehicle vehicle = Vehicle.builder()
                .user(AppUser.builder().id(request.getUserId()).build())
                .licensePlate(request.getLicensePlate())
                .brand(request.getBrand())
                .model(request.getModel())
                .color(request.getColor())
                .status(swp391.carwash.enums.RecordStatus.ACTIVE) // Mặc định xe mới tạo sẽ ở trạng thái hoạt động
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return mapToResponse(savedVehicle);
    }
    @Override
    public List<VehicleResponse> getAll() {
        return vehicleRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<VehicleResponse> getByUserId(Integer userId) {
        return vehicleRepository
                .findVehicleByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public VehicleResponse update(Integer vehicleId, UpdateVehicleRequest request) {
        Vehicle vehicle = findVehicleById(vehicleId);

        if (!vehicle.getLicensePlate().equals(request.getLicensePlate())) {
            if (vehicleRepository.existsByLicensePlateAndDeletedAtIsNull(request.getLicensePlate())) {
                throw new RuntimeException("Biển số xe mới này đã được sử dụng ở một phương tiện khác!");
            }
        }

        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setColor(request.getColor());
        vehicle.setStatus(swp391.carwash.enums.RecordStatus.valueOf(request.getStatus()));

        Vehicle updatedVehicle =vehicleRepository.save(vehicle);

        return mapToResponse(updatedVehicle);
    }


    @Override
    public void delete(Integer vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        vehicle.setStatus(swp391.carwash.enums.RecordStatus.DELETED);
        vehicle.setDeletedAt(OffsetDateTime.now());

        vehicleRepository.delete(vehicle);
    }

    @Override
    public List<VehicleResponse> getByEmail(String email) {
        // 1. Tìm thông tin User/Customer dựa vào email lấy từ Token
        // Giả sử bạn có userRepository được inject ở trên, nếu chưa có hãy add vào nhé
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        // 2. Tìm danh sách xe theo ID của User đó từ Repository
        List<Vehicle> vehicles = vehicleRepository.findVehicleByUserId(user.getId());

        // 3. Chuyển đổi từ List<Vehicle> sang List<VehicleResponse> bằng Stream API
        return vehicles.stream()
                .map(vehicle -> VehicleResponse.builder()
                        .vehicleId(vehicle.getId())
                        .licensePlate(vehicle.getLicensePlate())
                        .brand(vehicle.getBrand())
                        .color(vehicle.getColor())
                        // Thêm các trường khác tùy thuộc vào thuộc tính thực tế trong DTO của bạn
                        .build())
                .toList(); // Hoặc .collect(Collectors.toList()) nếu bạn dùng Java bản cũ hơn 16
    }

    private Vehicle findVehicleById(Integer vehicleId) {
        return vehicleRepository
                .findByIdAndDeletedAtIsNull(vehicleId).orElseThrow(() ->
                        new RuntimeException("không tìm thấy phương tiện này"));
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getId())
                .userId(vehicle.getUser().getId() != null ? vehicle.getUser().getId() : null)
                .licensePlate(vehicle.getLicensePlate())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .status(vehicle.getStatus().name())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}
