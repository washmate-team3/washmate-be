package swp391.carwash.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.respone.vehicles.NotificationResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification System", description = "APIs quản lý và cập nhật trạng thái thông báo của tài khoản")
public class NotificationController {

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo của tài khoản đang đăng nhập (Sắp xếp mới nhất trước)")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Integer userId = 1; // Lấy từ JWT
        // TODO: Gọi sang notificationService.getUserNotifications(userId, page, size)
        return ResponseEntity.ok(List.of());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo cụ thể là đã đọc")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id) {
        // TODO: Gọi sang notificationService.markAsRead(id)
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu đọc tất cả thông báo của tài khoản hiện tại")
    public ResponseEntity<Void> markAllAsRead() {
        Integer userId = 1; // Lấy từ JWT
        // TODO: Gọi sang notificationService.markAllAsRead(userId)
        return ResponseEntity.ok().build();
    }
}
