package swp391.carwash.dto;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.util.StringUtils;

public record OtpRequest(
        String emailOrPhone,
        String email,
        String phone
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

    @AssertTrue(message = "emailOrPhone, email, or phone is required")
    private boolean isIdentifierPresent() {
        return StringUtils.hasText(emailOrPhone)
                || StringUtils.hasText(email)
                || StringUtils.hasText(phone);
    }
}
