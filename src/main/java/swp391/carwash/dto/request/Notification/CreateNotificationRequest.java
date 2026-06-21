package swp391.carwash.dto.request.Notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationRequest(
        @NotNull(message = "ID người nhận không được để trống")
        Integer userId,

        Integer bookingId,

        @NotBlank(message = "Tiêu đề không được để trống")
        String title,

        @NotBlank(message = "Nội dung không được để trống")
        String content,

        @NotBlank(message = "Loại thông báo không được để trống")
        String type, // BOOKING_CONFIRMATION, LOYALTY_UPDATE...

        String channel // Mặc định truyền IN_APP
) {}