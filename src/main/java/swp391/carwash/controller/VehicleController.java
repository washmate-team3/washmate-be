package swp391.carwash.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.VehicleCreateRequest;
import swp391.carwash.dto.VehicleResponse;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.VehicleService;

@RestController
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService vehicleService;

    @GetMapping("/api/vehicles")
    public List<VehicleResponse> getMyVehicles(@AuthenticationPrincipal AppUserDetails principal) {
        return vehicleService.getMyVehicles(principal);
    }

    @PostMapping("/api/vehicles")
    public VehicleResponse createVehicle(
            @Valid @RequestBody VehicleCreateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return vehicleService.createVehicle(request, principal);
    }
}
