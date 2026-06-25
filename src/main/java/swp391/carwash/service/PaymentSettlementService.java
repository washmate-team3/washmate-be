package swp391.carwash.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.Invoice;
import swp391.carwash.entity.Payment;
import swp391.carwash.entity.PaymentTransaction;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.enums.InvoiceStatus;
import swp391.carwash.enums.PaymentMethod;
import swp391.carwash.enums.PaymentStatus;
import swp391.carwash.enums.PaymentTransactionStatus;
import swp391.carwash.repository.InvoiceRepository;
import swp391.carwash.event.InvoiceCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSettlementService {
    private final InvoiceRepository invoiceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private static final Set<BookingStatus> SETTLEABLE_BOOKING_STATUSES = EnumSet.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN,
            BookingStatus.WASHING);

    public Invoice settle(
            Payment payment,
            Booking booking,
            PaymentTransaction transaction,
            PaymentMethod method,
            OffsetDateTime now) {
        if (!SETTLEABLE_BOOKING_STATUSES.contains(booking.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Only active booking payment can be confirmed");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PENDING payment can be confirmed");
        }
        if (payment.getAmount().compareTo(booking.getFinalAmount()) != 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment amount does not match booking final amount");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setMethod(method);
        payment.setPaidAt(now);
        payment.setUpdatedAt(now);

        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setConfirmedAt(now);
        }

        transaction.setStatus(PaymentTransactionStatus.SUCCESS);
        transaction.setAmount(payment.getAmount());

        Invoice invoice = invoiceRepository.findByBookingId(booking.getId()).orElseGet(() -> invoiceRepository.save(
                Invoice.builder()
                        .invoiceCode(generateInvoiceCode())
                        .booking(booking)
                        .payment(payment)
                        .garage(booking.getGarage())
                        .subtotal(booking.getTotalAmount())
                        .discount(booking.getDiscountAmount())
                        .penaltyTotal(BigDecimal.ZERO)
                        .totalAmount(booking.getFinalAmount())
                        .status(InvoiceStatus.PAID)
                        .issuedAt(now)
                        .paidAt(now)
                        .build()));

        if (invoice.getPayment() == null) {
            invoice.setPayment(payment);
        } else if (!invoice.getPayment().getId().equals(payment.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Invoice belongs to another payment");
        }
        if (!invoice.getGarage().getId().equals(payment.getGarage().getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Invoice and payment garage do not match");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(invoice.getPaidAt() == null ? now : invoice.getPaidAt());

        // Publish event to trigger email
        eventPublisher.publishEvent(new InvoiceCreatedEvent(invoice.getId()));
        
        log.info("Payment settled successfully. Payment {}, Booking {}, Invoice {}", payment.getId(), booking.getBookingCode(), invoice.getInvoiceCode());

        return invoice;
    }

    private String generateInvoiceCode() {
        return "INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
