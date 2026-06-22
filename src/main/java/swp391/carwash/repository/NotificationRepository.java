package swp391.carwash.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.Notification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // Phân trang danh sách thông báo theo userId
    Page<Notification> findByUserId(Integer userId, Pageable pageable);

    // Tìm thông báo cụ thể thuộc quyền sở hữu của user (ngăn chặn đọc trộm dữ liệu chéo)
    Optional<Notification> findByNotificationIdAndUserId(Integer notificationId, Integer userId);

    // Update nhanh toàn bộ trạng thái chưa đọc thành đã đọc của 1 User
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.status = 'READ', n.readAt = :now " +
            "WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Integer userId, @Param("now") OffsetDateTime now);
}