package swp391.carwash.dto.request.redem;

import jakarta.validation.constraints.NotNull;

public record CustomerRedeemRequest(
        @NotNull(message = "Garage ID không được để trống")
        Integer garageId,

        // Cho phép null: Nếu null nghĩa là đặt lịch bình thường, không đổi quà
        Integer rewardId,

        @NotNull(message = "Booking ID không được để trống")
        Integer bookingId
) {}