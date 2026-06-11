package swp391.carwash.dto;

import swp391.carwash.enums.PaymentMethod;

public record PaymentConfirmRequest(
        PaymentMethod method,
        String provider,
        String providerTxnId
) {
}
