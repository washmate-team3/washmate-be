package swp391.carwash.dto.insight;

public record AIDeepAnalysisRejection(
        String claim,
        String metric,
        String reason
) {
}
