package swp391.carwash.dto;

import jakarta.validation.constraints.NotBlank;

public record VehicleCreateRequest(
        @NotBlank String licensePlate,
        String brand,
        String model,
        String color
) {
}
