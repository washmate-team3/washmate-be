package swp391.carwash.dto.insight;

import java.util.Map;

public record TrendPoint(
        String period,
        Map<String, Object> metrics
) {
}
