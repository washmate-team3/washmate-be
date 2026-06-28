package swp391.carwash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookingRejectRequest(
        @NotBlank
        @Size(max = 500)
        String reason
) {
}
