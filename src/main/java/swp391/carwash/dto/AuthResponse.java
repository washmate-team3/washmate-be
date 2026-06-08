package swp391.carwash.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserSummary user
) {
    public record UserSummary(Integer id, String email, String fullName, String phone, List<String> roles, List<Integer> garageIds) {
    }
}
