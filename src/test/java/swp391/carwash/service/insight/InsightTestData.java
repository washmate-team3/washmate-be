package swp391.carwash.service.insight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import swp391.carwash.dto.insight.InsightPeriod;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.BookingSlot;
import swp391.carwash.entity.Garage;
import swp391.carwash.entity.Invoice;
import swp391.carwash.entity.LoyaltyAccount;
import swp391.carwash.entity.LoyaltyTransaction;
import swp391.carwash.entity.RewardRedemption;
import swp391.carwash.entity.ServicePackage;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.enums.InvoiceStatus;
import swp391.carwash.enums.TransactionType;

public final class InsightTestData {
    private InsightTestData() {
    }

    public static InsightAnalysisContext context(InsightMetrics current, InsightMetrics previous) {
        return new InsightAnalysisContext(current, previous, OffsetDateTime.parse("2026-07-31T10:00:00+07:00"));
    }

    public static InsightMetrics metrics(
            LocalDate from,
            LocalDate to,
            List<Booking> bookings,
            List<Invoice> invoices,
            List<LoyaltyTransaction> loyaltyTransactions,
            List<RewardRedemption> redemptions,
            List<ServicePackage> services,
            Set<Integer> priorCustomerIds,
            long accountsWithPoints) {
        return new InsightMetrics(
                new InsightPeriod(from, to),
                bookings,
                invoices,
                loyaltyTransactions,
                redemptions,
                services,
                priorCustomerIds,
                accountsWithPoints);
    }

    public static Garage garage() {
        return Garage.builder()
                .id(1)
                .name("AutoWash")
                .address("Address")
                .phone("0900000000")
                .build();
    }

    public static AppUser customer(int id) {
        return AppUser.builder()
                .id(id)
                .fullName("Customer " + id)
                .phone("09000000" + id)
                .build();
    }

    public static AppUser staff(int id) {
        return AppUser.builder()
                .id(id)
                .fullName("Staff " + id)
                .phone("09100000" + id)
                .build();
    }

    public static ServicePackage service(int id, String name) {
        return ServicePackage.builder()
                .id(id)
                .garage(garage())
                .name(name)
                .price(new BigDecimal("100000"))
                .duration(30)
                .build();
    }

    public static BookingSlot slot(int id, LocalTime start, LocalTime end) {
        return BookingSlot.builder()
                .id(id)
                .garage(garage())
                .startTime(start)
                .endTime(end)
                .build();
    }

    public static Booking booking(int id, int customerId, BookingStatus status, LocalDate date) {
        return booking(id, customerId, status, date, service(1, "Rửa xe cơ bản"), slot(1, LocalTime.of(8, 0), LocalTime.of(9, 0)));
    }

    public static Booking booking(
            int id,
            int customerId,
            BookingStatus status,
            LocalDate date,
            ServicePackage service,
            BookingSlot slot) {
        return Booking.builder()
                .id(id)
                .bookingCode("BKG-" + id)
                .user(customer(customerId))
                .garage(garage())
                .slot(slot)
                .service(service)
                .bookingDate(date)
                .totalAmount(new BigDecimal("100000"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("100000"))
                .status(status)
                .build();
    }

    public static Invoice paidInvoice(int id, Booking booking, String amount, OffsetDateTime paidAt) {
        return Invoice.builder()
                .id(id)
                .invoiceCode("INV-" + id)
                .booking(booking)
                .garage(garage())
                .subtotal(new BigDecimal(amount))
                .discount(BigDecimal.ZERO)
                .penaltyTotal(BigDecimal.ZERO)
                .totalAmount(new BigDecimal(amount))
                .status(InvoiceStatus.PAID)
                .paidAt(paidAt)
                .build();
    }

    public static LoyaltyTransaction earnTransaction(int id, int customerId, int points, OffsetDateTime createdAt) {
        LoyaltyAccount account = LoyaltyAccount.builder()
                .id(100 + customerId)
                .user(customer(customerId))
                .garage(garage())
                .totalPoints(points)
                .availablePoints(points)
                .build();
        return LoyaltyTransaction.builder()
                .id(id)
                .account(account)
                .points(points)
                .transactionType(TransactionType.EARN)
                .earnedAt(createdAt)
                .createdAt(createdAt)
                .build();
    }

    public static RewardRedemption redemption(int id, int customerId, int points, String status, ZonedDateTime redeemedAt) {
        LoyaltyAccount account = LoyaltyAccount.builder()
                .id(200 + customerId)
                .user(customer(customerId))
                .garage(garage())
                .totalPoints(points)
                .availablePoints(points)
                .build();
        return RewardRedemption.builder()
                .redemptionId(id)
                .loyaltyAccount(account)
                .garage(garage())
                .pointsUsed(points)
                .status(status)
                .redeemedAt(redeemedAt)
                .build();
    }
}
