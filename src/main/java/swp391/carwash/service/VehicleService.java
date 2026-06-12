package swp391.carwash.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.VehicleCreateRequest;
import swp391.carwash.dto.VehicleResponse;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.Vehicle;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.VehicleRepository;
import swp391.carwash.security.AppUserDetails;

@Service
@RequiredArgsConstructor
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(AppUserDetails principal) {
        return vehicleRepository.findByUserIdAndStatusOrderByIdDesc(principal.getId(), RecordStatus.ACTIVE).stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional
    public VehicleResponse createVehicle(VehicleCreateRequest request, AppUserDetails principal) {
        AppUser user = appUserRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        Vehicle vehicle = Vehicle.builder()
                .user(user)
                .licensePlate(request.licensePlate())
                .brand(request.brand())
                .model(request.model())
                .color(request.color())
                .build();
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }
}
