package swp391.carwash.dto.request.redem;

import jakarta.validation.constraints.NotBlank;

public record UpdateRedemptionStatusRequest(
        @NotBlank(message = "Trạng thái xử lý không được để trống")
        String status // APPROVED, COMPLETED, REJECTED
) {}