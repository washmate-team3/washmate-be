package swp391.carwash.dto;

import java.time.OffsetDateTime;

import lombok.Builder;
import swp391.carwash.entity.LoyaltyTransaction;
import swp391.carwash.enums.TransactionType;

public record LoyaltyTransactionResponse(
        Integer id,
        Integer accountId,
        Integer garageId,
        String garageName,
        Integer bookingId,
        Integer sourceTransactionId,
        Integer points,
        TransactionType transactionType,
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
