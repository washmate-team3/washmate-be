package swp391.carwash.dto;

import jakarta.validation.constraints.NotNull;
import swp391.carwash.enums.UserStatus;

public record UpdateUserStatusRequest(
        @NotNull UserStatus status
) {
}
