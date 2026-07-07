package swp391.carwash.service.Loyalty.Support;

import java.time.OffsetDateTime;

public record QuarterPeriod(
        OffsetDateTime start,
        OffsetDateTime end
) {
}