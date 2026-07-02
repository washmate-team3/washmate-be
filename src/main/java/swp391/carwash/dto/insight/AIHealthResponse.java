package swp391.carwash.dto.insight;

public record AIHealthResponse(
        boolean configured,
        String model,
        String promptVersion,
        String message
) {
}
