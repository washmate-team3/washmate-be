package swp391.carwash.dto.insight;

import java.time.LocalDate;

public record AIDeepAnalysisRequest(
        LocalDate fromDate,
        LocalDate toDate,
        Integer garageId
) {
}
