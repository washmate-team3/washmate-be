package swp391.carwash.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.BookingResponse;
import swp391.carwash.dto.PaymentActionRequest;
import swp391.carwash.dto.PaymentConfirmRequest;
import swp391.carwash.dto.PaymentResponse;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.Invoice;
import swp391.carwash.entity.Payment;
import swp391.carwash.entity.PaymentTransaction;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.enums.InvoiceStatus;
import swp391.carwash.enums.PaymentStatus;
import swp391.carwash.enums.PaymentTransactionStatus;
import swp391.carwash.repository.BookingRepository;
import swp391.carwash.repository.InvoiceRepository;
import swp391.carwash.repository.PaymentRepository;
import swp391.carwash.repository.PaymentTransactionRepository;
import swp391.carwash.security.AppUserDetails;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Integer paymentId, AppUserDetails principal) {
        Payment payment = findDetailedPayment(paymentId);
        authorizeBookingRead(payment.getBooking(), principal);
        return PaymentResponse.from(payment, paymentTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBooking(Integer bookingId, AppUserDetails principal) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        authorizeBookingRead(payment.getBooking(), principal);
        return PaymentResponse.from(payment, paymentTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(payment.getId()));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse.TransactionInfo> getPaymentTransactions(Integer paymentId, AppUserDetails principal) {
        Payment payment = findDetailedPayment(paymentId);
        authorizeBookingRead(payment.getBooking(), principal);
        return PaymentResponse.from(payment, paymentTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId)).transactions();
    }

    @Transactional
    public BookingResponse confirmPayment(Integer paymentId, PaymentConfirmRequest request, AppUserDetails principal) {
        Payment payment = findDetailedPayment(paymentId);
        Booking booking = findDetailedBooking(payment.getBooking().getId());
        authorizeGarageOperation(booking, principal);

        return confirmPendingPayment(payment, booking, request);
    }

    @Transactional
    public BookingResponse confirmCustomerPayment(Integer paymentId, PaymentConfirmRequest request, AppUserDetails principal) {
        Payment payment = findDetailedPayment(paymentId);
        Booking booking = findDetailedBooking(payment.getBooking().getId());
        authorizeCustomerPayment(booking, principal);

        return confirmPendingPayment(payment, booking, request);
    }

    private BookingResponse confirmPendingPayment(Payment payment, Booking booking, PaymentConfirmRequest request) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PENDING booking can be confirmed");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PENDING payment can be confirmed");
        }
        if (payment.getAmount().compareTo(booking.getFinalAmount()) != 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment amount does not match booking final amount");
        }

        String provider = providerOrManual(request == null ? null : request.provider());
        String providerTxnId = request == null ? null : request.providerTxnId();
        ensureProviderTransactionIsNew(provider, providerTxnId);

        OffsetDateTime now = OffsetDateTime.now();
        payment.setStatus(PaymentStatus.PAID);
        payment.setMethod(request == null || request.method() == null ? payment.getMethod() : request.method());
        payment.setPaidAt(now);
        payment.setUpdatedAt(now);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(now);
        paymentRepository.flush();

        recordPaymentTransaction(payment, PaymentTransactionStatus.SUCCESS, provider, providerTxnId);
        paymentTransactionRepository.flush();

        Invoice invoice = invoiceRepository.findByBookingId(booking.getId()).orElseGet(() -> invoiceRepository.save(Invoice.builder()
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
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(invoice.getPaidAt() == null ? now : invoice.getPaidAt());

        return BookingResponse.from(booking, payment, invoice);
    }

    @Transactional
    public BookingResponse failPayment(Integer paymentId, PaymentActionRequest request, AppUserDetails principal) {
        return closePendingPayment(paymentId, request, principal, PaymentStatus.FAILED, PaymentTransactionStatus.FAILED, "Only PENDING payment can be failed");
    }

    @Transactional
    public BookingResponse cancelPayment(Integer paymentId, PaymentActionRequest request, AppUserDetails principal) {
        return closePendingPayment(paymentId, request, principal, PaymentStatus.CANCELLED, PaymentTransactionStatus.CANCELLED, "Only PENDING payment can be cancelled");
    }

    @Transactional
    public BookingResponse refundPayment(Integer paymentId, PaymentActionRequest request, AppUserDetails principal) {
        Payment payment = findDetailedPayment(paymentId);
        Booking booking = findDetailedBooking(payment.getBooking().getId());
        authorizeGarageOperation(booking, principal);

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PAID payment can be refunded");
        }

        String provider = providerOrManual(request == null ? null : request.provider());
        String providerTxnId = request == null ? null : request.providerTxnId();
        ensureProviderTransactionIsNew(provider, providerTxnId);

        OffsetDateTime now = OffsetDateTime.now();
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(now);
        recordPaymentTransaction(payment, PaymentTransactionStatus.REFUNDED, provider, providerTxnId);

        Invoice invoice = invoiceRepository.findByBookingId(booking.getId()).orElse(null);
        if (invoice != null) {
            invoice.setStatus(InvoiceStatus.REFUNDED);
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(now);
        }

        return BookingResponse.from(booking, payment, invoice);
    }

    private BookingResponse closePendingPayment(
            Integer paymentId,
            PaymentActionRequest request,
            AppUserDetails principal,
            PaymentStatus paymentStatus,
            PaymentTransactionStatus transactionStatus,
            String invalidStatusMessage) {
        Payment payment = findDetailedPayment(paymentId);
        Booking booking = findDetailedBooking(payment.getBooking().getId());
        authorizeGarageOperation(booking, principal);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, invalidStatusMessage);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PENDING booking payment can be closed without refund");
        }

        String provider = providerOrManual(request == null ? null : request.provider());
        String providerTxnId = request == null ? null : request.providerTxnId();
        ensureProviderTransactionIsNew(provider, providerTxnId);

        OffsetDateTime now = OffsetDateTime.now();
        payment.setStatus(paymentStatus);
        payment.setUpdatedAt(now);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        recordPaymentTransaction(payment, transactionStatus, provider, providerTxnId);

        Invoice invoice = invoiceRepository.findByBookingId(booking.getId()).orElse(null);
        return BookingResponse.from(booking, payment, invoice);
    }

    private Payment findDetailedPayment(Integer paymentId) {
        return paymentRepository.findDetailedById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    private Booking findDetailedBooking(Integer bookingId) {
        return bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private void authorizeBookingRead(Booking booking, AppUserDetails principal) {
        if (booking.getUser().getId().equals(principal.getId()) || canOperateGarage(booking, principal)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this payment");
    }

    private void authorizeGarageOperation(Booking booking, AppUserDetails principal) {
        if (!canOperateGarage(booking, principal)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot operate this payment");
        }
    }

    private void authorizeCustomerPayment(Booking booking, AppUserDetails principal) {
        if (booking.getUser().getId().equals(principal.getId()) && principal.getRoleNames().contains("CUSTOMER")) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "You cannot confirm this payment");
    }

    private boolean canOperateGarage(Booking booking, AppUserDetails principal) {
        List<String> roles = principal.getRoleNames();
        if (roles.contains("ADMIN") || roles.contains("OWNER")) {
            return true;
        }
        return (roles.contains("STAFF") || roles.contains("MANAGER"))
                && principal.getGarageIds().contains(booking.getGarage().getId());
    }

    private void ensureProviderTransactionIsNew(String provider, String providerTxnId) {
        if (providerTxnId == null || providerTxnId.isBlank()) {
            return;
        }
        if (paymentTransactionRepository.existsByProviderAndProviderTxnId(provider, providerTxnId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Provider transaction already exists");
        }
    }

    private void recordPaymentTransaction(Payment payment, PaymentTransactionStatus status, String provider, String providerTxnId) {
        try {
            paymentTransactionRepository.save(PaymentTransaction.builder()
                    .payment(payment)
                    .provider(provider)
                    .providerTxnId(providerTxnId)
                    .amount(payment.getAmount())
                    .status(status)
                    .build());
            paymentTransactionRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "Provider transaction already exists");
        }
    }

    private String providerOrManual(String provider) {
        return provider == null || provider.isBlank() ? "MANUAL" : provider;
    }

    private String generateInvoiceCode() {
        return "INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
