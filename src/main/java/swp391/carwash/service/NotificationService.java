package swp391.carwash.service;

import swp391.carwash.dto.response.Notification.NotificationResponse;
import swp391.carwash.dto.response.PagedResponse;

public interface NotificationService {
    PagedResponse<NotificationResponse> getUserNotifications(Integer userId, int page, int size);
    void markAsRead(Integer notificationId, Integer userId);
    void markAllAsRead(Integer userId);
}