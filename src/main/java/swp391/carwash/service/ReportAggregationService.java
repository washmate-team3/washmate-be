package swp391.carwash.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.insight.InsightPeriod;
import swp391.carwash.entity.ServicePackage;
import swp391.carwash.enums.InvoiceStatus;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.repository.BookingRepository;
import swp391.carwash.repository.InvoiceRepository;
import swp391.carwash.repository.LoyaltyAccountRepository;
import swp391.carwash.repository.LoyaltyTransactionRepository;
import swp391.carwash.repository.RewardRedemptionRepository;
import swp391.carwash.repository.ServicePackageRepository;
import swp391.carwash.service.insight.InsightAnalysisContext;
import swp391.carwash.service.insight.InsightMetrics;

@Service
@RequiredArgsConstructor
public class ReportAggregationService {
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
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

        InsightMetrics current = loadSnapshot(
                fromDate,
                toDate,
                zone,
                new HashSet<>(bookingRepository.findDistinctCustomerIdsWithBookingBefore(fromDate, garageId)),
                activeServices,
                accountsWithAvailablePoints,
                garageId);
        InsightMetrics previous = loadSnapshot(
                previousFrom,
                previousTo,
                zone,
                new HashSet<>(bookingRepository.findDistinctCustomerIdsWithBookingBefore(previousFrom, garageId)),
                activeServices,
                accountsWithAvailablePoints,
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
                accountsWithAvailablePoints);
    }
}
