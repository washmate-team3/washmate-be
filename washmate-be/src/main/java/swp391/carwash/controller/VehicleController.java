package swp391.carwash.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.request.vehicles.CreateVehicleRequest;
import swp391.carwash.dto.request.vehicles.UpdateVehicleRequest;
import swp391.carwash.dto.respone.vehicles.VehicleResponse;
import swp391.carwash.service.VehicleService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor

public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @Tag(name = "Imoprt API", description = "thêm xe dựac vào id_user")
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody CreateVehicleRequest request) {

        VehicleResponse response =vehicleService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Tag(name = "Find API", description = "tìm kiếm tất cả xe có trong hệ thống")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {

        return ResponseEntity.ok(vehicleService.getAll()
        );
    }

    @GetMapping("/user/{userId}")
    @Tag(name = "Find API", description = "tìm kiếm xe bằng id của xe")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByUserId(
            @PathVariable Integer userId) {
        return ResponseEntity.ok(
                vehicleService.getByUserId(userId)
        );
    }

    @PutMapping("/{vehicleId}")
    @Tag(name = "Update API", description = "cập nhập xe bằng id của xe")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Integer vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request) {

        VehicleResponse response = vehicleService.update(vehicleId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{vehicleId}")
    @Tag(name = "delete API", description = "xóa theo id của xe")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Integer vehicleId) {

        vehicleService.delete(vehicleId);

        return ResponseEntity.noContent().build();
    }

}
