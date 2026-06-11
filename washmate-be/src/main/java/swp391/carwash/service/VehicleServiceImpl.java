package swp391.carwash.service;


import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.request.vehicles.CreateVehicleRequest;
import swp391.carwash.dto.request.vehicles.UpdateVehicleRequest;
import swp391.carwash.dto.respone.vehicles.VehicleResponse;

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
                .status("ACTIVE") // Mặc định xe mới tạo sẽ ở trạng thái hoạt động
                .createdAt(OffsetDateTime.now()) // Thời gian tạo là hiện tại
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
                .findById(userId)
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
        vehicle.setStatus(request.getStatus());

        Vehicle updatedVehicle =vehicleRepository.save(vehicle);

        return mapToResponse(updatedVehicle);
    }


    @Override
    public void delete(Integer vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        vehicle.setStatus("DELETED");
        vehicle.setDeletedAt(OffsetDateTime.now());

        vehicleRepository.delete(vehicle);
    }

    private Vehicle findVehicleById(Integer vehicleId) {
        return vehicleRepository
                .findByIdAndDeletedAtIsNull(vehicleId).orElseThrow(() ->
                        new RuntimeException("không tìm thấy phương tiện này"));
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getId())
                .userId(vehicle.getId() != null ? vehicle.getId() : null)
                .licensePlate(vehicle.getLicensePlate())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .status(vehicle.getStatus())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}
