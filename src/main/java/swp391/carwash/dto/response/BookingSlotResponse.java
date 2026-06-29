package swp391.carwash.dto.response;

import java.time.LocalTime;

public record BookingSlotResponse(
        Integer slotId,
        Integer garageId,
        LocalTime startTime,
        LocalTime endTime,
        Integer maxCapacity,
        Long bookedCapacity,
        Long availableCapacity,
        Boolean available
) {
}
