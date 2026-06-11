package swp391.carwash.dto.request.vehicles;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateVehicleRequest {

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Model is required")
    private String model;

    @NotBlank(message = "Color is required")
    private String color;

    @NotBlank(message = "Status is required")
    private String status;
}