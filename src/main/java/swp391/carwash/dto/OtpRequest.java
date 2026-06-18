package swp391.carwash.dto;

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
}
