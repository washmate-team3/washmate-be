package swp391.carwash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.util.StringUtils;

public record OtpVerifyRequest(
        String emailOrPhone,
        String email,
        String phone,
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$")
        String otp,
        String fullName
) {
    public String identifier() {
        if (StringUtils.hasText(emailOrPhone)) {
            return emailOrPhone;
        }
        if (StringUtils.hasText(email)) {
            return email;
        }
        return phone;
    }
}
