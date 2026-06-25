package swp391.carwash.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface BookingBehaviorMonthlyView {
    Integer getUserId();
    Integer getGarageId();
    String getMonthYear();
    Integer getTotalBookings();
    Integer getCompletedCount();
    Integer getCancelledCount();
    Integer getNoShowCount();
    BigDecimal getTotalSpent();
    Integer getPreferredSlotId();
    String getStatus();
    OffsetDateTime getLastUpdated();
}
