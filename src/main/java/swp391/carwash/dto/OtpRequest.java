package swp391.carwash.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpRequest(@NotBlank String phone) {
}
