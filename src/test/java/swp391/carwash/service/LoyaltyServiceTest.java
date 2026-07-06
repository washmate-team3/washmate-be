package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.Garage;
import swp391.carwash.entity.LoyaltyAccount;
import swp391.carwash.entity.LoyaltyTransaction;
import swp391.carwash.entity.MembershipTier;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.enums.TransactionType;
import swp391.carwash.repository.LoyaltyAccountRepository;
import swp391.carwash.repository.LoyaltyTransactionRepository;
import swp391.carwash.repository.MembershipTierRepository;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {
    @Mock
    private LoyaltyAccountRepository loyaltyAccountRepository;
    @Mock
    private LoyaltyTransactionRepository loyaltyTransactionRepository;
    @Mock
    private MembershipTierRepository membershipTierRepository;

    @InjectMocks
    private LoyaltyService loyaltyService;

    private Booking booking;
    private Garage garage;
    private AppUser customer;
    private LoyaltyAccount account;
    private MembershipTier tier;

    @BeforeEach
    void setUp() {
        garage = Garage.builder().id(1).build();
        customer = AppUser.builder().id(10).build();
        
        tier = MembershipTier.builder().id(1).garage(garage).minPoints(0).status(RecordStatus.ACTIVE).build();

        booking = Booking.builder()
                .id(100)
                .user(customer)
                .garage(garage)
                .finalAmount(new BigDecimal("50000.00")) // 50000 / 10000 = 5 points
                .build();

        account = LoyaltyAccount.builder()
                .id(1)
                .user(customer)
                .garage(garage)
                .tier(tier)
                .totalPoints(10)
                .availablePoints(10)
                .build();
    }

    @Test
    void accruePointsCalculatesAndSavesProperly() {
        when(loyaltyTransactionRepository.existsByBookingIdAndTransactionType(100,TransactionType.EARN)).thenReturn(false);
        when(loyaltyAccountRepository.findByUserIdAndGarageId(10, 1)).thenReturn(Optional.of(account));
        when(membershipTierRepository.findFirstByGarageIdAndStatusAndMinPointsLessThanEqualOrderByMinPointsDesc(1, RecordStatus.ACTIVE, 15))
                .thenReturn(Optional.of(tier));

        loyaltyService.accruePoints(booking);

        assertEquals(15, account.getTotalPoints());
        assertEquals(15, account.getAvailablePoints());
        verify(loyaltyTransactionRepository).save(any(LoyaltyTransaction.class));
    }

    @Test
    void accruePointsDoesNotRunTwice() {
        when(loyaltyTransactionRepository.existsByBookingIdAndTransactionType(100, TransactionType.EARN)).thenReturn(true);

        loyaltyService.accruePoints(booking);

        verify(loyaltyAccountRepository, never()).save(any());
        verify(loyaltyTransactionRepository, never()).save(any());
    }

    @Test
    void accruePointsNoPointsForZeroAmount() {
        booking.setFinalAmount(BigDecimal.ZERO);
        when(loyaltyTransactionRepository.existsByBookingIdAndTransactionType(100, TransactionType.EARN)).thenReturn(false);

        loyaltyService.accruePoints(booking);

        verify(loyaltyAccountRepository, never()).save(any());
    }

    @Test
    void accruePointsSkipsWhenGarageHasNoActiveTier() {
        when(loyaltyTransactionRepository.existsByBookingIdAndTransactionType(100, TransactionType.EARN)).thenReturn(false);
        when(loyaltyAccountRepository.findByUserIdAndGarageId(10, 1)).thenReturn(Optional.empty());
        when(membershipTierRepository.findFirstByGarageIdAndStatusOrderByMinPointsAsc(1, RecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        loyaltyService.accruePoints(booking);

        verify(loyaltyAccountRepository, never()).save(any());
        verify(loyaltyTransactionRepository, never()).save(any());
    }

    @Test
    void rollbackEarnedPointsReducesPointsCorrectly() {
        LoyaltyTransaction earned = LoyaltyTransaction.builder()
                .id(99)
                .account(account)
                .points(5)
                .build();

        when(loyaltyTransactionRepository.findByBookingIdAndTransactionType(100, TransactionType.EARN)).thenReturn(Optional.of(earned));
        when(loyaltyTransactionRepository.existsBySourceTransactionIdAndTransactionType(99, TransactionType.ROLLBACK)).thenReturn(false);
        when(membershipTierRepository.findFirstByGarageIdAndStatusAndMinPointsLessThanEqualOrderByMinPointsDesc(1, RecordStatus.ACTIVE, 5))
                .thenReturn(Optional.of(tier));

        loyaltyService.rollbackEarnedPointsForBooking(booking);

        assertEquals(5, account.getTotalPoints());
        assertEquals(5, account.getAvailablePoints());
        verify(loyaltyTransactionRepository).save(any(LoyaltyTransaction.class));
    }

    @Test
    void rollbackEarnedPointsDoesNotGoBelowZero() {
        account.setTotalPoints(2);
        account.setAvailablePoints(2);
        LoyaltyTransaction earned = LoyaltyTransaction.builder()
                .id(99)
                .account(account)
                .points(5) // User earned 5, but current balance is 2 (maybe spent some)
                .build();

        when(loyaltyTransactionRepository.findByBookingIdAndTransactionType(100, TransactionType.EARN)).thenReturn(Optional.of(earned));
        when(loyaltyTransactionRepository.existsBySourceTransactionIdAndTransactionType(99, TransactionType.ROLLBACK)).thenReturn(false);
        when(membershipTierRepository.findFirstByGarageIdAndStatusAndMinPointsLessThanEqualOrderByMinPointsDesc(1, RecordStatus.ACTIVE, 0))
                .thenReturn(Optional.of(tier));

        loyaltyService.rollbackEarnedPointsForBooking(booking);

        assertEquals(0, account.getTotalPoints());
        assertEquals(0, account.getAvailablePoints());
    }
}
