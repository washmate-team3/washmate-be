package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swp391.carwash.entity.Notification;
import swp391.carwash.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setNotificationId(100);
        notification.setUserId(10);
        notification.setRead(false);
        notification.setStatus("UNREAD");
    }

    @Test
    void markAsReadSuccess() {
        when(notificationRepository.findByNotificationIdAndUserId(100, 10))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(100, 10);

        assertTrue(notification.isRead());
        assertEquals("READ", notification.getStatus());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadThrowsWhenNotFoundOrNotOwner() {
        when(notificationRepository.findByNotificationIdAndUserId(100, 99))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> notificationService.markAsRead(100, 99));

        assertEquals("Không tìm thấy thông báo hoặc bạn không có quyền sở hữu", exception.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllAsReadCallsRepositoryMethod() {
        notificationService.markAllAsRead(10);

        verify(notificationRepository).markAllAsReadByUserId(org.mockito.ArgumentMatchers.eq(10), any(OffsetDateTime.class));
    }
}
