package swp391.carwash.dto;

import jakarta.validation.constraints.Size;

public record PaymentActionRequest(
        @Size(max = 50)
        String provider,
        @Size(max = 255)
        String providerTxnId,
        @Size(max = 500)
        String reason
) {
}
