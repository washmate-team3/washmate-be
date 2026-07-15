package swp391.carwash.dto.response;

import java.util.List;

/**
 * Bản xem trước chiến dịch gửi khách từ một insight (read-only, chưa gửi).
 * BE sinh nháp email + đề xuất voucher; owner xem/sửa ở FE trước khi gửi.
 */
public record CampaignPreviewResponse(
        String ruleCode,
        int targetCount,
        List<String> sampleEmails,
        String subject,
        String body,
        String suggestedDiscountType,
        Integer suggestedDiscountValue,
        boolean aiGenerated
) {
}
