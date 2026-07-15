package swp391.carwash.dto.response;

/**
 * Kết quả gửi chiến dịch: số email đã gửi + mã voucher đã tạo.
 */
public record CampaignSendResponse(
        int sentCount,
        int failedCount,
        String voucherCode
) {
}
