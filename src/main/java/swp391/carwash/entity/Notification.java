    package swp391.carwash.entity;

    import jakarta.persistence.*;
    import lombok.*;
    import org.hibernate.annotations.CreationTimestamp;
    import java.time.OffsetDateTime;

    @Entity
    @Table(name = "notification", schema = "public")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class Notification {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "notification_id")
        private Integer notificationId;

        // Liên kết với bảng app_user (Có thể map tạm bằng ID hoặc Mối quan hệ ManyToOne)
        @Column(name = "user_id", nullable = false)
        private Integer userId;

        @Column(name = "booking_id")
        private Integer bookingId;

        @Column(name = "title", nullable = false)
        private String title;

        @Column(name = "content", nullable = false)
        private String content;

        @Column(name = "type", nullable = false)
        private String type; // BOOKING_CONFIRMATION, BOOKING_REMINDER, LOYALTY_UPDATE, PROMOTION, SYSTEM

        @Column(name = "channel", nullable = false)
        private String channel = "IN_APP"; // IN_APP, EMAIL, SMS, ZALO

        @Column(name = "status", nullable = false)
        private String status = "PENDING"; // PENDING, SENT, FAILED, READ

        @Column(name = "is_read", nullable = false)
        private boolean isRead = false;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private OffsetDateTime createdAt;

        @Column(name = "sent_at")
        private OffsetDateTime sentAt;

        @Column(name = "read_at")
        private OffsetDateTime readAt;
    }