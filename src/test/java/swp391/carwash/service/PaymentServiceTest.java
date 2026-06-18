package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.PaymentActionRequest;
import swp391.carwash.dto.PaymentConfirmRequest;
import swp391.carwash.dto.PaymentResponse;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.BookingSlot;
import swp391.carwash.entity.Garage;
import swp391.carwash.entity.Invoice;
import swp391.carwash.entity.Payment;
import swp391.carwash.entity.PaymentTransaction;
import swp391.carwash.entity.ServicePackage;
import swp391.carwash.entity.Vehicle;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.enums.InvoiceStatus;
import swp391.carwash.enums.PaymentMethod;
import swp391.carwash.enums.PaymentStatus;
import swp391.carwash.repository.BookingRepository;
import swp391.carwash.repository.InvoiceRepository;
import swp391.carwash.repository.PaymentRepository;
import swp391.carwash.repository.PaymentTransactionRepository;
import swp391.carwash.security.AppUserDetails;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private LoyaltyService loyaltyService;
    @Mock
    private AppUserDetails principal;

    private PaymentService paymentService;
    private Booking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(bookingRepository, invoiceRepository, paymentRepository, paymentTransactionRepository, loyaltyService);

        Garage garage = Garage.builder().id(1).name("Garage 1").address("Address").phone("0900000000").build();
        AppUser customer = AppUser.builder().id(10).fullName("Customer").phone("0911111111").build();
        Vehicle vehicle = Vehicle.builder().id(20).user(customer).licensePlate("59A1-12345").build();
        BookingSlot slot = BookingSlot.builder().id(30).garage(garage).build();
        ServicePackage service = ServicePackage.builder()
                .id(40)
                .garage(garage)
                .name("Basic Wash")
                .price(new BigDecimal("50000.00"))
                .duration(30)
                .build();

        booking = Booking.builder()
                .id(100)
                .bookingCode("BKG-TEST")
                .user(customer)
                .garage(garage)
                .slot(slot)
                .service(service)
                .vehicle(vehicle)
                .bookingDate(LocalDate.now())
                .totalAmount(new BigDecimal("50000.00"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("50000.00"))
                .status(BookingStatus.PENDING)
                .build();

        payment = Payment.builder()
                .id(200)
                .booking(booking)
                .garage(garage)
                .amount(new BigDecimal("50000.00"))
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PENDING)
                .build();

        lenient().when(principal.getRoleNames()).thenReturn(List.of("ADMIN"));
    }

    @Test
    void confirmPaymentRejectsAmountMismatch() {
        payment.setAmount(new BigDecimal("49000.00"));
        when(paymentRepository.findDetailedById(200)).thenReturn(Optional.of(payment));
        when(bookingRepository.findDetailedById(100)).thenReturn(Optional.of(booking));

        ApiException exception = assertThrows(ApiException.class,
                () -> paymentService.confirmPayment(200, new PaymentConfirmRequest(PaymentMethod.CASH, "MANUAL", "TXN-1"), principal));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Payment amount does not match booking final amount", exception.getMessage());
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void confirmPaymentRejectsDuplicateProviderTransaction() {
        when(paymentRepository.findDetailedById(200)).thenReturn(Optional.of(payment));
        when(bookingRepository.findDetailedById(100)).thenReturn(Optional.of(booking));
        when(paymentTransactionRepository.existsByProviderAndProviderTxnId("MANUAL", "TXN-1")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> paymentService.confirmPayment(200, new PaymentConfirmRequest(PaymentMethod.CASH, "MANUAL", "TXN-1"), principal));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Provider transaction already exists", exception.getMessage());
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void refundPaidCompletedBookingKeepsBookingCompletedAndRefundsInvoice() {
        booking.setStatus(BookingStatus.COMPLETED);
        payment.setStatus(PaymentStatus.PAID);
        Invoice invoice = Invoice.builder()
                .id(300)
                .invoiceCode("INV-TEST")
                .booking(booking)
                .payment(payment)
                .garage(booking.getGarage())
                .subtotal(booking.getTotalAmount())
                .discount(BigDecimal.ZERO)
                .penaltyTotal(BigDecimal.ZERO)
                .totalAmount(booking.getFinalAmount())
                .status(InvoiceStatus.PAID)
                .build();

        when(paymentRepository.findDetailedById(200)).thenReturn(Optional.of(payment));
        when(bookingRepository.findDetailedById(100)).thenReturn(Optional.of(booking));
        when(paymentTransactionRepository.existsByProviderAndProviderTxnId("MANUAL", "REF-1")).thenReturn(false);
        when(invoiceRepository.findByBookingId(100)).thenReturn(Optional.of(invoice));

        paymentService.refundPayment(200, new PaymentActionRequest("MANUAL", "REF-1", "Customer requested refund"), principal);

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(InvoiceStatus.REFUNDED, invoice.getStatus());
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertSame(invoice, invoiceRepository.findByBookingId(100).orElseThrow());
        verify(loyaltyService).rollbackEarnedPointsForBooking(booking);
    }

    @Test
    void customerCanConfirmOwnPendingPayment() {
        when(principal.getId()).thenReturn(10);
        when(principal.getRoleNames()).thenReturn(List.of("CUSTOMER"));
        when(paymentRepository.findDetailedById(200)).thenReturn(Optional.of(payment));
        when(bookingRepository.findDetailedById(100)).thenReturn(Optional.of(booking));
        when(paymentTransactionRepository.existsByProviderAndProviderTxnId("VNPAY", "VNPAY-1")).thenReturn(false);
        when(invoiceRepository.findByBookingId(100)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.confirmCustomerPayment(200, new PaymentConfirmRequest(PaymentMethod.VNPAY, "VNPAY", "VNPAY-1"), principal);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(PaymentMethod.VNPAY, payment.getMethod());
        assertNotNull(payment.getPaidAt());
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
    }

    @Test
    void customerCannotConfirmAnotherCustomersPayment() {
        when(principal.getId()).thenReturn(999);
        when(paymentRepository.findDetailedById(200)).thenReturn(Optional.of(payment));
        when(bookingRepository.findDetailedById(100)).thenReturn(Optional.of(booking));

        ApiException exception = assertThrows(ApiException.class,
                () -> paymentService.confirmCustomerPayment(200, new PaymentConfirmRequest(PaymentMethod.CARD, "CARD", "CARD-1"), principal));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("You cannot confirm this payment", exception.getMessage());
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void getPaymentByBookingReturnsPaymentForBookingOwner() {
        when(principal.getId()).thenReturn(10);
        when(paymentRepository.findByBookingId(100)).thenReturn(Optional.of(payment));
        when(paymentTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(200)).thenReturn(List.of());

        PaymentResponse response = paymentService.getPaymentByBooking(100, principal);

        assertEquals(200, response.id());
        assertEquals(100, response.bookingId());
        assertEquals(PaymentStatus.PENDING.name(), response.status());
    }
}
