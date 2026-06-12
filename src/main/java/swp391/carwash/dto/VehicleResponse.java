package swp391.carwash.dto;

import swp391.carwash.entity.Vehicle;

public record VehicleResponse(
        Integer id,
        String licensePlate,
        String brand,
        String model,
        String color,
        String status
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getStatus().name()
        );
    }
}
