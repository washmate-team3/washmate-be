package swp391.carwash.dto.insight;

import java.time.LocalDate;

public record InsightPeriod(
        LocalDate from,
        LocalDate to
) {
}
