package swp391.carwash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String identifier,
        @NotBlank String otp,
        @NotBlank @Size(min = 6, max = 100) String newPassword
) {
}
