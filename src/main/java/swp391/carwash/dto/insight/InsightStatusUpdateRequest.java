package swp391.carwash.dto.insight;

import jakarta.validation.constraints.NotNull;
import swp391.carwash.enums.InsightStatus;

public record InsightStatusUpdateRequest(
        @NotNull InsightStatus status
) {
}
