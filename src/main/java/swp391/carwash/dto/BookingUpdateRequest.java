package swp391.carwash.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookingUpdateRequest(
        @NotNull Integer garageId,
        @NotNull Integer slotId,
        @NotNull Integer serviceId,
        @NotNull Integer vehicleId,
        @NotNull @FutureOrPresent LocalDate bookingDate
) {
}
