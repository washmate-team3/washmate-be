package swp391.carwash.dto;

import java.util.List;
import swp391.carwash.entity.AppUser;
import swp391.carwash.enums.RecordStatus;

public record MeResponse(
        Integer id,
        String accountCode,
        String fullName,
        String email,
        String phone,
        String address,
        String avatarUrl,
        String role,
        String status,
        List<String> roles,
        List<Integer> garageIds
) {
    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "OWNER", "MANAGER", "STAFF", "CUSTOMER");

    public MeResponse(Integer id, String email, String fullName, String phone, String status, List<String> roles, List<Integer> garageIds) {
        this(
                id,
                accountCode(id),
                fullName,
                email,
                phone,
                null,
                null,
                primaryRole(roles),
                status,
                roles,
                garageIds
        );
    }

    public static MeResponse from(AppUser user) {
        List<String> roles = user.getUserRoles().stream()
                .filter(userRole -> userRole.getStatus() == RecordStatus.ACTIVE)
                .map(userRole -> userRole.getRole().getRoleName().name())
                .distinct()
                .toList();

        List<Integer> garageIds = user.getUserRoles().stream()
                .filter(userRole -> userRole.getStatus() == RecordStatus.ACTIVE)
                .filter(userRole -> userRole.getGarage() != null)
                .map(userRole -> userRole.getGarage().getId())
                .distinct()
                .toList();

        return new MeResponse(
                user.getId(),
                accountCode(user.getId()),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getAvatarUrl(),
                primaryRole(roles),
                user.getStatus().name(),
                roles,
                garageIds
        );
    }

    private static String accountCode(Integer userId) {
        return userId == null ? null : "WM-USER-%08d".formatted(userId);
    }

    private static String primaryRole(List<String> roles) {
        return ROLE_PRIORITY.stream()
                .filter(roles::contains)
                .findFirst()
                .orElse(roles.isEmpty() ? null : roles.get(0));
    }
}
