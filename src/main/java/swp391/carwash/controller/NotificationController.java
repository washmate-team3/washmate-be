package swp391.carwash.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.response.Notification.NotificationResponse;
import swp391.carwash.dto.response.PagedResponse;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification System", description = "APIs quản lý và cập nhật trạng thái thông báo của tài khoản")
public class NotificationController {

    // CHUẨN: Chỉ tiêm (Inject) tầng Service vào đây, không tiêm Repository trực tiếp
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo của tài khoản đang đăng nhập (Sắp xếp mới nhất trước)")
    public ResponseEntity<PagedResponse<NotificationResponse>> getMyNotifications(

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AppUserDetails principal

    ) {

        Integer userId = principal.getId();

        // Gọi Service thực thi quét DB và phân trang
        PagedResponse<NotificationResponse> response = notificationService.getUserNotifications(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo cụ thể là đã đọc")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id,
                                           @AuthenticationPrincipal AppUserDetails principal
    ) {
        Integer userId = principal.getId();

        // Gọi Service xử lý kiểm tra chính chủ và cập nhật trạng thái Đã Đọc
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build(); // Trả về 200 OK trống trải chuẩn RESTful
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu đọc tất cả thông báo của tài khoản hiện tại")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        Integer userId = principal.getId();

        // Gọi Service quét sạch đống thông báo chưa đọc của User này
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}