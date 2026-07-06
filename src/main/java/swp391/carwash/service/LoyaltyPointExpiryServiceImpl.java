package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.entity.LoyaltyAccount;
import swp391.carwash.entity.LoyaltyTransaction;
import swp391.carwash.enums.TransactionType;
import swp391.carwash.repository.LoyaltyAccountRepository;
import swp391.carwash.repository.LoyaltyTransactionRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LoyaltyPointExpiryServiceImpl
        implements LoyaltyPointExpiryService {

    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;

    @Override
    public void expirePoints() {

        List<LoyaltyTransaction> transactions =
                loyaltyTransactionRepository
                        .findByTransactionTypeAndExpiredFalseAndExpiresAtLessThanEqual(
                                TransactionType.EARN,
                                OffsetDateTime.now());

        for (LoyaltyTransaction transaction : transactions) {

            expire(transaction);

        }

    }

    private void expire(LoyaltyTransaction earnTransaction) {

        LoyaltyAccount account = earnTransaction.getAccount();

        int actualExpiredPoint = Math.min(
                account.getAvailablePoints(),
                earnTransaction.getPoints());

        if (actualExpiredPoint <= 0) {
            earnTransaction.setExpired(true);
            loyaltyTransactionRepository.save(earnTransaction);
            return;
        }

        account.setAvailablePoints(
                account.getAvailablePoints() - actualExpiredPoint);

        loyaltyAccountRepository.save(account);

        earnTransaction.setExpired(true);
        loyaltyTransactionRepository.save(earnTransaction);

        LoyaltyTransaction expireTransaction = LoyaltyTransaction.builder()
                .account(account)
                .points(-actualExpiredPoint)
                .transactionType(TransactionType.EXPIRE)
                .description("Điểm hết hạn")
                .createdAt(OffsetDateTime.now())
                .build();

        loyaltyTransactionRepository.save(expireTransaction);
    }
}
