package swp391.carwash.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import swp391.carwash.entity.Payment;
import swp391.carwash.entity.PaymentTransaction;

public record PaymentResponse(
        Integer id,
        Integer bookingId,
        Integer garageId,
        BigDecimal amount,
        String method,
        String status,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<TransactionInfo> transactions
) {
    public static PaymentResponse from(Payment payment, List<PaymentTransaction> transactions) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getGarage().getId(),
                payment.getAmount(),
                payment.getMethod().name(),
                payment.getStatus().name(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                transactions == null ? List.of() : transactions.stream().map(TransactionInfo::from).toList()
        );
    }

    public record TransactionInfo(
            Integer id,
            String provider,
            String providerTxnId,
            BigDecimal amount,
            String status,
            OffsetDateTime createdAt
    ) {
        static TransactionInfo from(PaymentTransaction transaction) {
            return new TransactionInfo(
                    transaction.getId(),
                    transaction.getProvider(),
                    transaction.getProviderTxnId(),
                    transaction.getAmount(),
                    transaction.getStatus().name(),
                    transaction.getCreatedAt()
            );
        }
    }
}
