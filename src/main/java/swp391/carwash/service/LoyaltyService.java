package swp391.carwash.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.LoyaltyAccountResponse;
import swp391.carwash.dto.LoyaltyTransactionResponse;
import swp391.carwash.repository.LoyaltyAccountRepository;
import swp391.carwash.repository.LoyaltyTransactionRepository;
import swp391.carwash.repository.MembershipTierRepository;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.LoyaltyAccount;
import swp391.carwash.entity.LoyaltyTransaction;
import swp391.carwash.entity.MembershipTier;
import swp391.carwash.enums.RecordStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class LoyaltyService {
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final MembershipTierRepository membershipTierRepository;

    @Transactional(readOnly = true)
    public List<LoyaltyAccountResponse> getMyAccounts(AppUserDetails principal) {
        return loyaltyAccountRepository.findByUserIdOrderByGarageNameAsc(principal.getId()).stream()
                .map(LoyaltyAccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponse> getMyTransactions(AppUserDetails principal) {
        return loyaltyTransactionRepository.findByAccountUserIdOrderByCreatedAtDesc(principal.getId()).stream()
                .map(LoyaltyTransactionResponse::from)
                .toList();
    }

    @Transactional
    public void accruePoints(Booking booking) {
        if (loyaltyTransactionRepository.existsByBookingIdAndTransactionType(booking.getId(), "EARN")) {
            return;
        }
        if (booking.getFinalAmount() == null || booking.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        int pointsToAdd = booking.getFinalAmount().divide(new BigDecimal("10000"), java.math.RoundingMode.DOWN).intValue();
        if (pointsToAdd <= 0) {
            return;
        }

        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdAndGarageId(booking.getUser().getId(), booking.getGarage().getId())
                .orElseGet(() -> {
                    MembershipTier tier = membershipTierRepository.findFirstByGarageIdAndStatusOrderByMinPointsAsc(booking.getGarage().getId(), RecordStatus.ACTIVE)
                            .orElse(null);
                    LoyaltyAccount newAccount = LoyaltyAccount.builder()
                            .user(booking.getUser())
                            .garage(booking.getGarage())
                            .tier(tier)
                            .totalPoints(0)
                            .availablePoints(0)
                            .status(RecordStatus.ACTIVE)
                            .createdAt(OffsetDateTime.now())
                            .build();
                    return loyaltyAccountRepository.save(newAccount);
                });

        account.setTotalPoints(account.getTotalPoints() + pointsToAdd);
        account.setAvailablePoints(account.getAvailablePoints() + pointsToAdd);
        membershipTierRepository.findFirstByGarageIdAndStatusAndMinPointsLessThanEqualOrderByMinPointsDesc(
                        booking.getGarage().getId(),
                        RecordStatus.ACTIVE,
                        account.getTotalPoints())
                .ifPresent(account::setTier);
        account.setUpdatedAt(OffsetDateTime.now());
        loyaltyAccountRepository.save(account);

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .account(account)
                .booking(booking)
                .points(pointsToAdd)
                .transactionType("EARN")
                .description("Earned from booking " + booking.getBookingCode())
                .earnedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        loyaltyTransactionRepository.save(transaction);
    }

    @Transactional
    public void rollbackEarnedPointsForBooking(Booking booking) {
        loyaltyTransactionRepository.findByBookingIdAndTransactionType(booking.getId(), "EARN")
                .ifPresent(earned -> {
                    if (loyaltyTransactionRepository.existsBySourceTransactionIdAndTransactionType(earned.getId(), "ROLLBACK")) {
                        return;
                    }

                    LoyaltyAccount account = earned.getAccount();
                    int rollbackPoints = Math.abs(earned.getPoints());
                    account.setAvailablePoints(Math.max(account.getAvailablePoints() - rollbackPoints, 0));
                    account.setTotalPoints(Math.max(account.getTotalPoints() - rollbackPoints, 0));
                    membershipTierRepository.findFirstByGarageIdAndStatusAndMinPointsLessThanEqualOrderByMinPointsDesc(
                                    booking.getGarage().getId(),
                                    RecordStatus.ACTIVE,
                                    account.getTotalPoints())
                            .ifPresent(account::setTier);
                    account.setUpdatedAt(OffsetDateTime.now());
                    loyaltyAccountRepository.save(account);

                    LoyaltyTransaction rollback = LoyaltyTransaction.builder()
                            .account(account)
                            .booking(booking)
                            .sourceTransaction(earned)
                            .points(-rollbackPoints)
                            .transactionType("ROLLBACK")
                            .description("Rollback earned points after payment refund")
                            .earnedAt(OffsetDateTime.now())
                            .createdAt(OffsetDateTime.now())
                            .build();
                    loyaltyTransactionRepository.save(rollback);
                });
    }
}
