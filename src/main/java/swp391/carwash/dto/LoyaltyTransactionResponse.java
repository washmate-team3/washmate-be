package swp391.carwash.dto;

import java.time.OffsetDateTime;
import swp391.carwash.entity.LoyaltyTransaction;

public record LoyaltyTransactionResponse(
        Integer id,
        Integer accountId,
        Integer garageId,
        String garageName,
        Integer bookingId,
        Integer sourceTransactionId,
        Integer points,
        String transactionType,
        String description,
        OffsetDateTime earnedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
) {
    public static LoyaltyTransactionResponse from(LoyaltyTransaction transaction) {
        return new LoyaltyTransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getAccount().getGarage().getId(),
                transaction.getAccount().getGarage().getName(),
                transaction.getBooking() == null ? null : transaction.getBooking().getId(),
                transaction.getSourceTransaction() == null ? null : transaction.getSourceTransaction().getId(),
                transaction.getPoints(),
                transaction.getTransactionType(),
                transaction.getDescription(),
                transaction.getEarnedAt(),
                transaction.getExpiresAt(),
                transaction.getCreatedAt()
        );
    }
}
