package swp391.carwash.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookingSlotCapacityUpdateRequest(
        @NotNull @Positive Integer maxCapacity
) {
}
