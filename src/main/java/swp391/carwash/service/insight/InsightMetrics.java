package swp391.carwash.service.insight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import swp391.carwash.dto.insight.InsightPeriod;
import swp391.carwash.dto.insight.InsightSummary;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.Invoice;
import swp391.carwash.entity.LoyaltyTransaction;
import swp391.carwash.entity.RewardRedemption;
import swp391.carwash.entity.ServicePackage;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.enums.TransactionType;

public class InsightMetrics {
    private static final Set<BookingStatus> UNSUCCESSFUL_STATUSES = EnumSet.of(
            BookingStatus.CANCELLED,
            BookingStatus.REJECTED,
            BookingStatus.NO_SHOW);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final InsightPeriod period;
    private final List<Booking> bookings;
    private final List<Invoice> paidInvoices;
    private final List<LoyaltyTransaction> loyaltyTransactions;
    private final List<RewardRedemption> rewardRedemptions;
    private final List<ServicePackage> activeServices;
    private final Set<Integer> customerIdsBeforePeriod;
    private final long loyaltyAccountsWithAvailablePoints;
    private final LoyaltyStateSnapshot loyaltyState;

    private List<ServiceStats> serviceStats;
    private List<TimeSlotStats> timeSlotStats;

    public InsightMetrics(
            InsightPeriod period,
            List<Booking> bookings,
            List<Invoice> paidInvoices,
            List<LoyaltyTransaction> loyaltyTransactions,
            List<RewardRedemption> rewardRedemptions,
            List<ServicePackage> activeServices,
            Set<Integer> customerIdsBeforePeriod,
            long loyaltyAccountsWithAvailablePoints,
            LoyaltyStateSnapshot loyaltyState) {
        this.period = period;
        this.bookings = List.copyOf(bookings);
        this.paidInvoices = List.copyOf(paidInvoices);
        this.loyaltyTransactions = List.copyOf(loyaltyTransactions);
        this.rewardRedemptions = List.copyOf(rewardRedemptions);
        this.activeServices = List.copyOf(activeServices);
        this.customerIdsBeforePeriod = Set.copyOf(customerIdsBeforePeriod);
        this.loyaltyAccountsWithAvailablePoints = loyaltyAccountsWithAvailablePoints;
        this.loyaltyState = loyaltyState == null ? LoyaltyStateSnapshot.empty() : loyaltyState;
    }

    public InsightPeriod period() {
        return period;
    }

    public List<Booking> bookings() {
        return bookings;
    }

    public List<Invoice> paidInvoices() {
        return paidInvoices;
    }

    public List<LoyaltyTransaction> loyaltyTransactions() {
        return loyaltyTransactions;
    }

    public List<RewardRedemption> rewardRedemptions() {
        return rewardRedemptions;
    }

    public long totalOrders() {
        return bookings.size();
    }

    public long completedOrders() {
        return bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.COMPLETED)
                .count();
    }

    public long cancelledOrders() {
        return bookings.stream()
                .filter(booking -> booking.getStatus() != null)
                .filter(booking -> UNSUCCESSFUL_STATUSES.contains(booking.getStatus()))
                .count();
    }

    public BigDecimal totalRevenue() {
        return paidInvoices.stream()
                .map(Invoice::getTotalAmount)
                .map(InsightMetrics::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long totalPointsEarned() {
        return loyaltyTransactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.EARN)                .map(LoyaltyTransaction::getPoints)
                .filter(points -> points != null && points > 0)
                .mapToLong(Integer::longValue)
                .sum();
    }

    public long totalPointsRedeemed() {
        long redemptionPoints = rewardRedemptions.stream()
                .filter(this::countsAsRedeemed)
                .map(RewardRedemption::getPointsUsed)
                .filter(points -> points != null && points > 0)
                .mapToLong(Integer::longValue)
                .sum();

        long transactionRedeemPoints = loyaltyTransactions.stream()
                .filter(transaction -> transaction.getRedemptionId() == null)
                .filter(transaction -> transaction.getTransactionType() == TransactionType.REDEEM)                .map(LoyaltyTransaction::getPoints)
                .filter(points -> points != null)
                .mapToLong(points -> Math.abs(points.longValue()))
                .sum();

        return redemptionPoints + transactionRedeemPoints;
    }

    public BigDecimal completedOrderRate() {
        return percentage(completedOrders(), totalOrders());
    }

    public BigDecimal cancelledOrderRate() {
        return percentage(cancelledOrders(), totalOrders());
    }

    public double completedOrderRatePercent() {
        return percentageDouble(completedOrders(), totalOrders());
    }

    public double cancelledOrderRatePercent() {
        return percentageDouble(cancelledOrders(), totalOrders());
    }

    public double revenueChangePercent(InsightMetrics previous) {
        return changePercent(totalRevenue(), previous.totalRevenue());
    }

    public double orderChangePercent(InsightMetrics previous) {
        return changePercent(BigDecimal.valueOf(totalOrders()), BigDecimal.valueOf(previous.totalOrders()));
    }

    public BigDecimal averageRevenuePerOrder() {
        if (totalOrders() == 0) {
            return BigDecimal.ZERO;
        }
        return totalRevenue().divide(BigDecimal.valueOf(totalOrders()), 2, RoundingMode.HALF_UP);
    }

    public boolean hasBusinessData() {
        return totalOrders() > 0
                || totalRevenue().compareTo(BigDecimal.ZERO) > 0
                || totalPointsEarned() > 0
                || totalPointsRedeemed() > 0;
    }

    public Set<Integer> customerIds() {
        return bookings.stream()
                .map(Booking::getUser)
                .filter(user -> user != null && user.getId() != null)
                .map(AppUser::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public long newCustomers() {
        return customerIds().stream()
                .filter(customerId -> !customerIdsBeforePeriod.contains(customerId))
                .count();
    }

    public long returningCustomers() {
        return customerIds().stream()
                .filter(customerIdsBeforePeriod::contains)
                .count();
    }

    public long returningCustomerOrders() {
        return bookings.stream()
                .map(Booking::getUser)
                .filter(user -> user != null && user.getId() != null)
                .filter(user -> customerIdsBeforePeriod.contains(user.getId()))
                .count();
    }

    public double returningOrderSharePercent() {
        return percentageDouble(returningCustomerOrders(), totalOrders());
    }

    public double newCustomerSharePercent() {
        return percentageDouble(newCustomers(), customerIds().size());
    }

    public Map<Integer, BigDecimal> revenueByCustomer() {
        return paidInvoices.stream()
                .filter(invoice -> invoice.getBooking() != null)
                .filter(invoice -> invoice.getBooking().getUser() != null)
                .filter(invoice -> invoice.getBooking().getUser().getId() != null)
                .collect(Collectors.toMap(
                        invoice -> invoice.getBooking().getUser().getId(),
                        invoice -> money(invoice.getTotalAmount()),
                        BigDecimal::add));
    }

    public double topCustomerRevenueSharePercent() {
        Map<Integer, BigDecimal> revenueByCustomer = revenueByCustomer();
        if (revenueByCustomer.size() < InsightThresholds.MIN_CUSTOMERS_FOR_CONCENTRATION
                || totalRevenue().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        int topCount = Math.max(1, (int) Math.ceil(revenueByCustomer.size() * 0.2));
        BigDecimal topRevenue = revenueByCustomer.values().stream()
                .sorted(Comparator.reverseOrder())
                .limit(topCount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return topRevenue.multiply(ONE_HUNDRED)
                .divide(totalRevenue(), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public List<ServiceStats> serviceStats() {
        if (serviceStats != null) {
            return serviceStats;
        }

        Map<Integer, ServicePackage> activeById = activeServices.stream()
                .filter(service -> service.getId() != null)
                .collect(Collectors.toMap(ServicePackage::getId, Function.identity(), (left, right) -> left));
        Map<Integer, Long> orderCountByService = bookings.stream()
                .filter(booking -> booking.getService() != null && booking.getService().getId() != null)
                .collect(Collectors.groupingBy(booking -> booking.getService().getId(), Collectors.counting()));
        Map<Integer, BigDecimal> revenueByService = paidInvoices.stream()
                .filter(invoice -> invoice.getBooking() != null)
                .filter(invoice -> invoice.getBooking().getService() != null)
                .filter(invoice -> invoice.getBooking().getService().getId() != null)
                .collect(Collectors.toMap(
                        invoice -> invoice.getBooking().getService().getId(),
                        invoice -> money(invoice.getTotalAmount()),
                        BigDecimal::add));

        Set<Integer> serviceIds = new LinkedHashSet<>();
        serviceIds.addAll(activeById.keySet());
        serviceIds.addAll(orderCountByService.keySet());
        serviceIds.addAll(revenueByService.keySet());

        serviceStats = serviceIds.stream()
                .map(serviceId -> new ServiceStats(
                        serviceId,
                        serviceName(serviceId),
                        orderCountByService.getOrDefault(serviceId, 0L),
                        revenueByService.getOrDefault(serviceId, BigDecimal.ZERO)))
                .toList();
        return serviceStats;
    }

    public Optional<ServiceStats> topServiceByUsage() {
        return serviceStats().stream()
                .filter(stats -> stats.orderCount() > 0)
                .max(Comparator.comparingLong(ServiceStats::orderCount));
    }

    public Optional<ServiceStats> topServiceByRevenue() {
        return serviceStats().stream()
                .filter(stats -> stats.revenue().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(ServiceStats::revenue));
    }

    public Optional<ServiceStats> lowUsageService() {
        return serviceStats().stream()
                .filter(stats -> activeServices.stream()
                        .anyMatch(service -> service.getId() != null && service.getId().equals(stats.serviceId())))
                .min(Comparator.comparingLong(ServiceStats::orderCount));
    }

    public List<TimeSlotStats> timeSlotStats() {
        if (timeSlotStats != null) {
            return timeSlotStats;
        }

        Map<String, Long> counts = bookings.stream()
                .filter(booking -> booking.getStatus() == null || !UNSUCCESSFUL_STATUSES.contains(booking.getStatus()))
                .filter(booking -> booking.getSlot() != null)
                .collect(Collectors.groupingBy(this::slotLabel, Collectors.counting()));

        timeSlotStats = counts.entrySet().stream()
                .map(entry -> new TimeSlotStats(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(TimeSlotStats::label))
                .toList();
        return timeSlotStats;
    }

    public Optional<TimeSlotStats> peakTimeSlot() {
        return timeSlotStats().stream()
                .max(Comparator.comparingLong(TimeSlotStats::orderCount));
    }

    public Optional<TimeSlotStats> lowTimeSlot() {
        return timeSlotStats().stream()
                .min(Comparator.comparingLong(TimeSlotStats::orderCount));
    }

    public long weekendDayCount() {
        return datesInPeriod().stream()
                .filter(this::isWeekend)
                .count();
    }

    public long weekdayDayCount() {
        return datesInPeriod().stream()
                .filter(date -> !isWeekend(date))
                .count();
    }

    public BigDecimal weekendRevenue() {
        return paidInvoices.stream()
                .filter(invoice -> invoice.getPaidAt() != null)
                .filter(invoice -> isWeekend(invoice.getPaidAt().toLocalDate()))
                .map(Invoice::getTotalAmount)
                .map(InsightMetrics::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal weekdayRevenue() {
        return paidInvoices.stream()
                .filter(invoice -> invoice.getPaidAt() != null)
                .filter(invoice -> !isWeekend(invoice.getPaidAt().toLocalDate()))
                .map(Invoice::getTotalAmount)
                .map(InsightMetrics::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal averageWeekendRevenuePerDay() {
        return averagePerDay(weekendRevenue(), weekendDayCount());
    }

    public BigDecimal averageWeekdayRevenuePerDay() {
        return averagePerDay(weekdayRevenue(), weekdayDayCount());
    }

    public Set<Integer> redeemedCustomerIds() {
        Set<Integer> customerIds = rewardRedemptions.stream()
                .filter(this::countsAsRedeemed)
                .map(RewardRedemption::getLoyaltyAccount)
                .filter(account -> account != null && account.getUser() != null && account.getUser().getId() != null)
                .map(account -> account.getUser().getId())
                .collect(Collectors.toCollection(HashSet::new));

        loyaltyTransactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.REDEEM)                .map(LoyaltyTransaction::getAccount)
                .filter(account -> account != null && account.getUser() != null && account.getUser().getId() != null)
                .map(account -> account.getUser().getId())
                .forEach(customerIds::add);

        return customerIds;
    }

    public long loyaltyAccountsWithAvailablePoints() {
        return loyaltyAccountsWithAvailablePoints;
    }

    public LoyaltyStateSnapshot loyaltyState() {
        return loyaltyState;
    }

    public long pointsExpiringSoon() {
        return loyaltyState.pointsExpiringSoon();
    }

    public long accountsWithExpiringPoints() {
        return loyaltyState.accountsWithExpiringPoints();
    }

    public long customersNearNextTier() {
        return loyaltyState.customersNearNextTier();
    }

    public long tierDowngradeCount() {
        return loyaltyState.downgradeCount();
    }

    public long totalActiveLoyaltyAccounts() {
        return loyaltyState.totalActiveAccounts();
    }

    public long lowestTierAccounts() {
        return loyaltyState.lowestTierAccounts();
    }

    public long inactiveCustomers() {
        return loyaltyState.inactiveCustomers();
    }

    public double tierDowngradeRatePercent() {
        return loyaltyState.downgradeRatePercent();
    }

    public double lowestTierSharePercent() {
        return loyaltyState.lowestTierSharePercent();
    }

    public double redemptionRateAgainstEarnedPointsPercent() {
        return percentageDouble(totalPointsRedeemed(), totalPointsEarned());
    }

    public double redemptionRateAgainstAccountsWithPointsPercent() {
        return percentageDouble(redeemedCustomerIds().size(), loyaltyAccountsWithAvailablePoints);
    }

    public double averageOrdersForCustomers(Set<Integer> targetCustomerIds) {
        if (targetCustomerIds.isEmpty()) {
            return 0;
        }
        Map<Integer, Long> orderCounts = bookings.stream()
                .filter(booking -> booking.getUser() != null && booking.getUser().getId() != null)
                .collect(Collectors.groupingBy(booking -> booking.getUser().getId(), Collectors.counting()));
        long totalOrdersForTargets = targetCustomerIds.stream()
                .map(orderCounts::get)
                .filter(count -> count != null)
                .mapToLong(Long::longValue)
                .sum();
        return (double) totalOrdersForTargets / targetCustomerIds.size();
    }

    public InsightSummary toSummary() {
        return new InsightSummary(
                totalRevenue(),
                totalOrders(),
                completedOrders(),
                cancelledOrders(),
                newCustomers(),
                returningCustomers(),
                totalPointsEarned(),
                totalPointsRedeemed(),
                completedOrderRate(),
                cancelledOrderRate(),
                topServiceByUsage().map(ServiceStats::serviceName).orElse(null),
                topServiceByRevenue().map(ServiceStats::serviceName).orElse(null),
                peakTimeSlot().map(TimeSlotStats::label).orElse(null),
                lowTimeSlot().map(TimeSlotStats::label).orElse(null));
    }

    private String serviceName(Integer serviceId) {
        return activeServices.stream()
                .filter(service -> service.getId() != null && service.getId().equals(serviceId))
                .map(ServicePackage::getName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .or(() -> bookings.stream()
                        .filter(booking -> booking.getService() != null)
                        .filter(booking -> serviceId.equals(booking.getService().getId()))
                        .map(booking -> booking.getService().getName())
                        .filter(name -> name != null && !name.isBlank())
                        .findFirst())
                .orElse("Dich vu #" + serviceId);
    }

    private String slotLabel(Booking booking) {
        if (booking.getSlot().getStartTime() == null || booking.getSlot().getEndTime() == null) {
            return "Khung gio #" + booking.getSlot().getId();
        }
        return booking.getSlot().getStartTime() + " - " + booking.getSlot().getEndTime();
    }

    private boolean countsAsRedeemed(RewardRedemption redemption) {
        if (redemption.getStatus() == null) {
            return true;
        }
        String status = redemption.getStatus().trim().toUpperCase();
        return !"REJECTED".equals(status) && !"CANCELLED".equals(status);
    }

    private List<LocalDate> datesInPeriod() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = period.from();
        while (!current.isAfter(period.to())) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private BigDecimal averagePerDay(BigDecimal value, long days) {
        if (days <= 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal percentage(long part, long whole) {
        if (whole <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    private static double percentageDouble(long part, long whole) {
        if (whole <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(part)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static double changePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return current.subtract(previous)
                .multiply(ONE_HUNDRED)
                .divide(previous, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public record ServiceStats(
            Integer serviceId,
            String serviceName,
            long orderCount,
            BigDecimal revenue
    ) {
        public double revenueSharePercent(BigDecimal totalRevenue) {
            if (totalRevenue == null || totalRevenue.compareTo(BigDecimal.ZERO) <= 0) {
                return 0;
            }
            return revenue.multiply(ONE_HUNDRED)
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        public double orderSharePercent(long totalOrders) {
            return percentageDouble(orderCount, totalOrders);
        }
    }

    public record TimeSlotStats(
            String label,
            long orderCount
    ) {
        public double orderSharePercent(long totalOrders) {
            return percentageDouble(orderCount, totalOrders);
        }
    }
}
