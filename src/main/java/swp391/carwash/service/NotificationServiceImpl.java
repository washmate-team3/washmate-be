package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.response.Notification.NotificationResponse;
import swp391.carwash.entity.Notification;
import swp391.carwash.repository.NotificationRepository;
import swp391.carwash.dto.response.PagedResponse;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUserNotifications(Integer userId, int page, int size) {
        // Cấu hình sắp xếp mới nhất lên trước theo trường createdAt đúng như Swagger mô tả
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationPage = notificationRepository.findByUserId(userId, pageable);

        // Convert danh sách từ Entity sang DTO công khai
        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PagedResponse.<NotificationResponse>builder()
                .content(content)
                .pageNo(notificationPage.getNumber())
                .pageSize(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .last(notificationPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(Integer notificationId, Integer userId) { // Đổi vị trí nhận ở đây
        // Bây giờ biến truyền vào Repository sẽ khớp hoàn toàn tuyệt đối với tên tham số
        Notification notification = notificationRepository.findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo hoặc bạn không có quyền sở hữu"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setStatus("READ");
            notification.setReadAt(OffsetDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsReadByUserId(userId, OffsetDateTime.now());
    }

    // Mapper thủ công giúp tối ưu hiệu năng
    private NotificationResponse mapToResponse(Notification entity) {
        return NotificationResponse.builder()
                .notificationId(entity.getNotificationId())
                .bookingId(entity.getBookingId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .channel(entity.getChannel())
                .status(entity.getStatus())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }
}