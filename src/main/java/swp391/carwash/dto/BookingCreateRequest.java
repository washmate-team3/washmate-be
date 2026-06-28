package swp391.carwash.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import swp391.carwash.enums.PaymentMethod;

public record BookingCreateRequest(
                @NotNull Integer garageId,
                @NotNull Integer slotId,
                @NotNull Integer serviceId,
                @NotNull Integer vehicleId,
                @NotNull @FutureOrPresent LocalDate bookingDate,
                Integer promotionId,
                PaymentMethod paymentMethod) {
}
