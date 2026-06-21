package swp391.carwash.service;

import swp391.carwash.dto.respone.Notification.NotificationResponse;
import swp391.carwash.dto.respone.PagedResponse;

public interface NotificationService {
    PagedResponse<NotificationResponse> getUserNotifications(Integer userId, int page, int size);
    void markAsRead(Integer notificationId, Integer userId);
    void markAllAsRead(Integer userId);
}