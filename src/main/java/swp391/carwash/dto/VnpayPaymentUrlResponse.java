package swp391.carwash.dto;

import java.time.OffsetDateTime;

public record VnpayPaymentUrlResponse(
        Integer paymentId,
        String paymentUrl,
        OffsetDateTime expiresAt
) {
}
