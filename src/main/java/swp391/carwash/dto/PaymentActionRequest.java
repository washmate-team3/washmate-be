package swp391.carwash.dto;

public record PaymentActionRequest(
        String provider,
        String providerTxnId,
        String reason
) {
}
