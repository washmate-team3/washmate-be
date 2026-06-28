package swp391.carwash.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank String fullName,
        String phone,
        String address
) {
    public UpdateProfileRequest(String fullName, String phone) {
        this(fullName, phone, null);
    }
}
