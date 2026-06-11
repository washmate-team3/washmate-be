package swp391.carwash.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpVerifyRequest(
        @NotBlank String phone,
        @NotBlank String otp,
        String fullName
) {
}
