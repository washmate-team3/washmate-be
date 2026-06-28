package swp391.carwash.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.util.StringUtils;

public record LoginRequest(
        String emailOrPhone,
        String email,
        @NotBlank String password
) {
    public String identifier() {
        if (StringUtils.hasText(emailOrPhone)) return emailOrPhone;
        if (StringUtils.hasText(email)) return email;
        return null;
    }
}
