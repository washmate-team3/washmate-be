package swp391.carwash.dto.respone.Garages;

import java.time.LocalDateTime;

public record GarageResponse(
        Integer garageId,
        String name,
        String address,
        String phone,
        String status,
        LocalDateTime createdAt
) {}
