package swp391.carwash.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.insight.InsightPeriod;
import swp391.carwash.entity.LoyaltyAccount;
import swp391.carwash.entity.MembershipTier;
import swp391.carwash.entity.ServicePackage;
import swp391.carwash.enums.InvoiceStatus;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.enums.TierChangeType;
import swp391.carwash.enums.TransactionType;
import swp391.carwash.repository.BookingRepository;
import swp391.carwash.repository.InvoiceRepository;
import swp391.carwash.repository.LoyaltyAccountRepository;
import swp391.carwash.repository.LoyaltyTierHistoryRepository;
import swp391.carwash.repository.LoyaltyTransactionRepository;
import swp391.carwash.repository.MembershipTierRepository;
import swp391.carwash.repository.RewardRedemptionRepository;
import swp391.carwash.repository.ServicePackageRepository;
import swp391.carwash.service.insight.InsightAnalysisContext;
import swp391.carwash.service.insight.InsightMetrics;
import swp391.carwash.service.insight.InsightThresholds;
import swp391.carwash.service.insight.LoyaltyStateSnapshot;

@Service
@RequiredArgsConstructor
public class ReportAggregationService {
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTierHistoryRepository loyaltyTierHistoryRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final ServicePackageRepository servicePackageRepository;

    @Transactional(readOnly = true)
    public InsightAnalysisContext aggregate(LocalDate fromDate, LocalDate toDate) {
        return aggregate(fromDate, toDate, null);
    }

    @Transactional(readOnly = true)
    public InsightAnalysisContext aggregate(LocalDate fromDate, LocalDate toDate, Integer garageId) {
        long periodDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousTo = fromDate.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(periodDays - 1);
        ZoneId zone = swp391.carwash.common.TimeZones.VIETNAM;

        List<ServicePackage> activeServices = servicePackageRepository.findActiveForInsightScope(
                RecordStatus.ACTIVE,
                garageId);
        long accountsWithAvailablePoints = loyaltyAccountRepository.countAccountsWithAvailablePointsForScope(
                RecordStatus.ACTIVE,
                garageId);

        OffsetDateTime currentFrom = fromDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime currentTo = toDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        LoyaltyStateSnapshot loyaltyState = computeLoyaltyState(currentFrom, currentTo, zone, garageId);

        InsightMetrics current = loadSnapshot(
                fromDate,
                toDate,
                zone,
                new HashSet<>(bookingRepository.findDistinctCustomerIdsWithBookingBefore(fromDate, garageId)),
                activeServices,
                accountsWithAvailablePoints,
                loyaltyState,
                garageId);
        InsightMetrics previous = loadSnapshot(
                previousFrom,
                previousTo,
                zone,
                new HashSet<>(bookingRepository.findDistinctCustomerIdsWithBookingBefore(previousFrom, garageId)),
                activeServices,
                accountsWithAvailablePoints,
                LoyaltyStateSnapshot.empty(),
                garageId);

        return new InsightAnalysisContext(current, previous, OffsetDateTime.now(zone));
    }

    private InsightMetrics loadSnapshot(
            LocalDate fromDate,
            LocalDate toDate,
            ZoneId zone,
            HashSet<Integer> customerIdsBeforePeriod,
            List<ServicePackage> activeServices,
            long accountsWithAvailablePoints,
            LoyaltyStateSnapshot loyaltyState,
            Integer garageId) {
        OffsetDateTime fromTime = fromDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime toTime = toDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        return new InsightMetrics(
                new InsightPeriod(fromDate, toDate),
                bookingRepository.findForInsightPeriod(fromDate, toDate, garageId),
                invoiceRepository.findPaidForInsightPeriod(InvoiceStatus.PAID, fromTime, toTime, garageId),
                loyaltyTransactionRepository.findForInsightPeriod(fromTime, toTime, garageId),
                rewardRedemptionRepository.findForInsightPeriod(fromTime, toTime, garageId),
                activeServices,
                customerIdsBeforePeriod,
                accountsWithAvailablePoints,
                loyaltyState);
    }

    /**
     * Tính trạng thái loyalty/tier hiện tại (bám khung đề): điểm sắp hết hạn,
     * khách sắp lên hạng, số lượt hạ hạng trong kỳ, và phân bố hạng.
     */
    private LoyaltyStateSnapshot computeLoyaltyState(
            OffsetDateTime periodFrom,
            OffsetDateTime periodTo,
            ZoneId zone,
            Integer garageId) {
        OffsetDateTime expiryThreshold = OffsetDateTime.now(zone)
                .plusDays(InsightThresholds.EXPIRING_WINDOW_DAYS);

        long pointsExpiringSoon = loyaltyTransactionRepository.sumExpiringPoints(
                TransactionType.EARN, expiryThreshold, garageId);
        long accountsWithExpiringPoints = loyaltyTransactionRepository.countAccountsWithExpiringPoints(
                TransactionType.EARN, expiryThreshold, garageId);
        long downgradeCount = loyaltyTierHistoryRepository.countByChangeTypeInPeriod(
                TierChangeType.DOWNGRADE, periodFrom, periodTo, garageId);

        List<LoyaltyAccount> accounts = loyaltyAccountRepository.findByStatus(RecordStatus.ACTIVE);
        Map<Integer, List<MembershipTier>> tiersByGarage = new HashMap<>();

        long totalActiveAccounts = 0;
        long lowestTierAccounts = 0;
        long customersNearNextTier = 0;

        for (LoyaltyAccount account : accounts) {
            if (account.getGarage() == null || account.getGarage().getId() == null || account.getTier() == null) {
                continue;
            }
            Integer accountGarageId = account.getGarage().getId();
            if (garageId != null && !garageId.equals(accountGarageId)) {
                continue;
            }
            totalActiveAccounts++;

            List<MembershipTier> tiers = tiersByGarage.computeIfAbsent(
                    accountGarageId,
                    gid -> membershipTierRepository.findByGarageIdAndStatusOrderByMinPointsAsc(gid, RecordStatus.ACTIVE));
            if (tiers == null || tiers.isEmpty()) {
                continue;
            }

            MembershipTier lowestTier = tiers.get(0);
            if (lowestTier.getId() != null && lowestTier.getId().equals(account.getTier().getId())) {
                lowestTierAccounts++;
            }

            MembershipTier nextTier = findNextTier(tiers, account.getTier());
            if (nextTier != null && nextTier.getMinPoints() != null && account.getTotalPoints() != null) {
                long gap = (long) nextTier.getMinPoints() - account.getTotalPoints();
                if (gap > 0 && gap <= InsightThresholds.UPGRADE_GAP_POINTS) {
                    customersNearNextTier++;
                }
            }
        }

        LocalDate inactiveCutoff = LocalDate.now(zone).minusDays(InsightThresholds.INACTIVE_DAYS);
        long inactiveCustomers = bookingRepository.findInactiveCustomerEmails(inactiveCutoff, garageId).size();

        return new LoyaltyStateSnapshot(
                pointsExpiringSoon,
                accountsWithExpiringPoints,
                customersNearNextTier,
                downgradeCount,
                totalActiveAccounts,
                lowestTierAccounts,
                inactiveCustomers);
    }

    private MembershipTier findNextTier(List<MembershipTier> tiersAsc, MembershipTier currentTier) {
        if (currentTier.getMinPoints() == null) {
            return null;
        }
        for (MembershipTier tier : tiersAsc) {
            if (tier.getMinPoints() != null && tier.getMinPoints() > currentTier.getMinPoints()) {
                return tier;
            }
        }
        return null;
    }
}
