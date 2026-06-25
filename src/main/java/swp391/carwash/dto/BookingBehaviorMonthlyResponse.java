package swp391.carwash.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import swp391.carwash.repository.BookingBehaviorMonthlyView;

public record BookingBehaviorMonthlyResponse(
        Integer userId,
        Integer garageId,
        String monthYear,
        Integer totalBookings,
        Integer completedCount,
        Integer cancelledCount,
        Integer noShowCount,
        BigDecimal totalSpent,
        Integer preferredSlotId,
        String status,
        OffsetDateTime lastUpdated
) {
    public static BookingBehaviorMonthlyResponse from(BookingBehaviorMonthlyView view) {
        return new BookingBehaviorMonthlyResponse(
                view.getUserId(),
                view.getGarageId(),
                view.getMonthYear(),
                view.getTotalBookings(),
                view.getCompletedCount(),
                view.getCancelledCount(),
                view.getNoShowCount(),
                view.getTotalSpent(),
                view.getPreferredSlotId(),
                view.getStatus(),
                view.getLastUpdated()
        );
    }
}
