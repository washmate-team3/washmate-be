package swp391.carwash.dto.response.vehicles;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Data
@Getter
@Setter
@Builder
public class VehicleResponse {

    private Integer vehicleId;

    private Integer userId;

    private String licensePlate;

    private String brand;

    private String model;

    private String color;

    private String status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    public VehicleResponse() {
    }

    public VehicleResponse(Integer vehicleId, Integer userId, String licensePlate, String brand, String model, String color, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.vehicleId = vehicleId;
        this.userId = userId;
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void setVehicleId(Integer vehicleId) {
        this.vehicleId = vehicleId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "VehicleResponse{" +
                "vehicleId=" + vehicleId +
                ", userId=" + userId +
                ", licensePlate='" + licensePlate + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", color='" + color + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}