package swp391.carwash.dto.response.Notification;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record NotificationResponse(
        Integer notificationId,
        Integer bookingId,
        String title,
        String content,
        String type,
        String channel,
        String status,
        boolean isRead,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
) {}