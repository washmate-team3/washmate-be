package swp391.carwash.dto.insight;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record InsightGenerateRequest(
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate
) {
}
