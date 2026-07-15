package swp391.carwash.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Yêu cầu gửi chiến dịch: owner đã duyệt nội dung. BE tạo voucher + gửi mail.
 */
public record CampaignSendRequest(
        @NotNull Integer garageId,
        @NotBlank String discountType,
        @NotNull @Positive Integer discountValue,
        @NotNull @Positive Integer voucherValidDays,
        @NotBlank String subject,
        @NotBlank String body
) {
}
