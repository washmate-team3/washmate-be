package swp391.carwash.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swp391.carwash.entity.LoyaltyTransaction;
import swp391.carwash.enums.TransactionType;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Integer> {
    @EntityGraph(attributePaths = {"account", "account.garage", "booking", "sourceTransaction"})
    List<LoyaltyTransaction> findByAccountUserIdOrderByCreatedAtDesc(Integer userId);

    boolean existsByBookingIdAndTransactionType(Integer bookingId, TransactionType transactionType);

    @EntityGraph(attributePaths = {"account"})
    Optional<LoyaltyTransaction> findByBookingIdAndTransactionType(Integer bookingId, TransactionType transactionType);

    boolean existsBySourceTransactionIdAndTransactionType(Integer sourceTransactionId, TransactionType transactionType);

    @EntityGraph(attributePaths = {"account", "account.user", "account.garage", "booking"})
    @Query("""
            select transaction from LoyaltyTransaction transaction
            where transaction.createdAt >= :fromTime
              and transaction.createdAt < :toTime
            """)
    List<LoyaltyTransaction> findForInsightPeriod(
            @Param("fromTime") OffsetDateTime fromTime,
            @Param("toTime") OffsetDateTime toTime);
}
