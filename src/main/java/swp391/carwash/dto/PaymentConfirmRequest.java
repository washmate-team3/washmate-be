package swp391.carwash.dto;

import jakarta.validation.constraints.Size;
import swp391.carwash.enums.PaymentMethod;

public record PaymentConfirmRequest(
        PaymentMethod method,
        @Size(max = 50)
        String provider,
        @Size(max = 255)
        String providerTxnId
) {
}
