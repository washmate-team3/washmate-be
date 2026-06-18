package swp391.carwash.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.util.StringUtils;

public record LoginRequest(
        String emailOrPhone,
        String email,
        @NotBlank String password
) {
    public String identifier() {
        return StringUtils.hasText(emailOrPhone) ? emailOrPhone : email;
    }
}
