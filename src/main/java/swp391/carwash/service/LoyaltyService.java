package swp391.carwash.service;

import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.LoyaltyAccountResponse;
import swp391.carwash.dto.LoyaltyTransactionResponse;
import swp391.carwash.entity.*;
import swp391.carwash.enums.TierChangeType;
import swp391.carwash.enums.TransactionType;
import swp391.carwash.repository.*;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.enums.RecordStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class LoyaltyService {
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final LoyaltyPolicyRepository loyaltyPolicyRepository;
    private final LoyaltyTierHistoryRepository loyaltyTierHistoryRepository;

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponse> getMyTransactions(AppUserDetails principal) {
        return loyaltyTransactionRepository.findByAccountUserIdOrderByCreatedAtDesc(principal.getId()).stream()
                .map(LoyaltyTransactionResponse::from)
                .toList();
    }

    @Transactional
        public void accruePoints(Booking booking) {
        validateBooking(booking);
        LoyaltyPolicy policy =getPolicy(booking.getGarage().getId());
        int point = calculatePoints(booking, policy);
        if (point <= 0) {return;}
        LoyaltyAccount account = getOrCreateAccount(booking,policy);
        updateAccount(account, point);
        saveEarnTransaction(account, booking, point);
        evaluateUpgrade(account);
    }

    private void evaluateUpgrade(LoyaltyAccount account) {

        MembershipTier currentTier = account.getTier();

        MembershipTier highestTier =
                membershipTierRepository
                        .findFirstByGarageIdAndStatusAndMinPointsLessThanEqualOrderByMinPointsDesc(
                                account.getGarage().getId(),
                                RecordStatus.ACTIVE,
                                account.getTotalPoints())
                        .orElse(currentTier);

        if (highestTier.getMinPoints() <= currentTier.getMinPoints()) {
            return;
        }

        account.setTier(highestTier);
        account.setUpdatedAt(OffsetDateTime.now());

        loyaltyAccountRepository.save(account);

        saveUpgradeHistory(account, currentTier, highestTier);
    }
    private void saveUpgradeHistory(
            LoyaltyAccount account,
            MembershipTier oldTier,
            MembershipTier newTier) {

        LoyaltyTierHistory history =
                LoyaltyTierHistory.builder()
                        .account(account)
                        .garage(account.getGarage())
                        .oldTier(oldTier)
                        .newTier(newTier)
                        .changeType(TierChangeType.UPGRADE)
                        .changeReason("Tự động nâng hạng khi đủ điểm")
                        .createdAt(OffsetDateTime.now())
                        .build();

        loyaltyTierHistoryRepository.save(history);
    }


    @Transactional
    public void rollbackEarnedPointsForBooking(Booking booking) {
        LoyaltyTransaction earned = getEarnTransaction(booking);
        validateRollback(earned);
        LoyaltyAccount account = earned.getAccount();
        rollbackAccount(account, earned);
        saveRollbackTransaction(account, booking, earned);
    }

    private void saveRollbackTransaction(
            LoyaltyAccount account,
            Booking booking,
            LoyaltyTransaction earned) {
        int rollbackPoints = Math.abs(earned.getPoints());
        OffsetDateTime now = OffsetDateTime.now();
        LoyaltyTransaction rollback =
                LoyaltyTransaction.builder()
                        .account(account)
                        .booking(booking)
                        .sourceTransaction(earned)
                        .points(-rollbackPoints)
                        .transactionType(TransactionType.ROLLBACK)
                        .description("Rollback earned points after payment refund")
                        .earnedAt(now)
                        .createdAt(now)
                        .build();

        loyaltyTransactionRepository.save(rollback);

    }

    private void rollbackAccount(
            LoyaltyAccount account,
            LoyaltyTransaction earned) {

        int rollbackPoints = Math.abs(earned.getPoints());

        account.setAvailablePoints(
                Math.max(
                        account.getAvailablePoints() - rollbackPoints,
                        0));

        account.setTotalPoints(
                Math.max(
                        account.getTotalPoints() - rollbackPoints,
                        0));

        account.setUpdatedAt(OffsetDateTime.now());

        loyaltyAccountRepository.save(account);

    }

    private void validateRollback(LoyaltyTransaction earned) {

        boolean rollbackExists =
                loyaltyTransactionRepository
                        .existsBySourceTransactionIdAndTransactionType(
                                earned.getId(),
                                TransactionType.ROLLBACK);

        if (rollbackExists) {
            throw new IllegalStateException(
                    "This loyalty transaction has already been rolled back.");
        }

    }

    private LoyaltyTransaction getEarnTransaction(Booking booking) {

        return loyaltyTransactionRepository
                .findByBookingIdAndTransactionType(booking.getId(), TransactionType.EARN)
                .orElseThrow(() ->
                        new IllegalStateException("No earned loyalty transaction found."));
    }

    private void validateBooking(Booking booking) {

        if (loyaltyTransactionRepository.existsByBookingIdAndTransactionType(
                booking.getId(),
                TransactionType.EARN)) {
            throw new IllegalStateException("Booking has already earned loyalty points.");
        }

        if (booking.getFinalAmount() == null
                || booking.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Booking amount is invalid.");
        }
    }
    private int calculatePoints(
            Booking booking,
            LoyaltyPolicy policy)
    {
        return booking.getFinalAmount()
                .divide(
                        policy.getAmountPerPoint(),
                        RoundingMode.DOWN)
                .intValue();
    }
    private LoyaltyAccount getOrCreateAccount(
            Booking booking,
            LoyaltyPolicy policy) {

        return loyaltyAccountRepository
                .findByUserIdAndGarageId(
                        booking.getUser().getId(),
                        booking.getGarage().getId())
                .orElseGet(() -> {
                    if (!policy.getAutoEnroll()) {
                        throw new IllegalStateException( "User is not enrolled in loyalty program.");
                    }
                    return createAccount(booking);
                });

    }
    private LoyaltyAccount createAccount(Booking booking) {

        MembershipTier defaultTier = membershipTierRepository
                .findFirstByGarageIdAndStatusOrderByMinPointsAsc(
                        booking.getGarage().getId(),
                        RecordStatus.ACTIVE)
                .orElseThrow(() ->
                        new IllegalStateException("Default membership tier not found."));

        LoyaltyAccount account = LoyaltyAccount.builder()
                .user(booking.getUser())
                .garage(booking.getGarage())
                .tier(defaultTier)
                .totalPoints(0)
                .availablePoints(0)
                .status(RecordStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        return loyaltyAccountRepository.save(account);
    }
    private void updateAccount(LoyaltyAccount account, int pointsToAdd) {

        account.setTotalPoints(account.getTotalPoints() + pointsToAdd);

        account.setAvailablePoints(account.getAvailablePoints() + pointsToAdd);

        account.setUpdatedAt(OffsetDateTime.now());

        loyaltyAccountRepository.save(account);
    }
    private void saveEarnTransaction(
            LoyaltyAccount account,
            Booking booking,
            int pointsToAdd) {

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .account(account)
                .booking(booking)
                .points(pointsToAdd)
                .transactionType(TransactionType.EARN)
                .description("Earned from booking " + booking.getBookingCode())
                .earnedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        loyaltyTransactionRepository.save(transaction);
    }
    private LoyaltyPolicy getPolicy(Integer garageId) {

        return loyaltyPolicyRepository
                .findByGarageIdAndStatus(
                        garageId,
                        RecordStatus.ACTIVE)
                .orElseThrow(() ->
                        new IllegalStateException("Loyalty policy not found."));
    }

}
