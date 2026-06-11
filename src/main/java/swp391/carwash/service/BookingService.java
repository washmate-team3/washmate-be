package swp391.carwash.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.BookingCreateRequest;
import swp391.carwash.dto.BookingResponse;
import swp391.carwash.entity.*;
import swp391.carwash.enums.*;
import swp391.carwash.repository.*;
import swp391.carwash.security.AppUserDetails;

@Service
@RequiredArgsConstructor
public class BookingService {
    private static final List<BookingStatus> CAPACITY_BLOCKING_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN,
            BookingStatus.WASHING
    );

    private final AppUserRepository appUserRepository;
    private final BookingRepository bookingRepository;
    private final BookingSlotRepository bookingSlotRepository;
    private final GarageRepository garageRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request, AppUserDetails principal) {
        requireCustomer(principal);
        AppUser customer = appUserRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        Garage garage = garageRepository.findById(request.garageId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Garage not found"));
        BookingSlot slot = bookingSlotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking slot not found"));
        ServicePackage service = servicePackageRepository.findById(request.serviceId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Service package not found"));
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        validateBookingInputs(request, customer, garage, slot, service, vehicle);

        BigDecimal totalAmount = service.getPrice();
        BigDecimal discountAmount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Discount amount is invalid");
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Discount is not supported yet");
        }
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode(generateBookingCode())
                .user(customer)
                .garage(garage)
                .slot(slot)
                .service(service)
                .vehicle(vehicle)
                .bookingDate(request.bookingDate())
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(BookingStatus.PENDING)
                .build());
        bookingRepository.flush();

        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElseGet(() -> paymentRepository.save(Payment.builder()
                .booking(booking)
                .garage(garage)
                .amount(finalAmount)
                .method(request.paymentMethod() == null ? PaymentMethod.CASH : request.paymentMethod())
                .status(PaymentStatus.PENDING)
                .build()));
        if (request.paymentMethod() != null && payment.getMethod() != request.paymentMethod()) {
            payment.setMethod(request.paymentMethod());
            payment.setUpdatedAt(OffsetDateTime.now());
        }

        return BookingResponse.from(booking, payment, null);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(AppUserDetails principal) {
        requireCustomer(principal);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(principal.getId()).stream()
                .map(booking -> BookingResponse.from(
                        booking,
                        paymentRepository.findByBookingId(booking.getId()).orElse(null),
                        invoiceRepository.findByBookingId(booking.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Integer bookingId, AppUserDetails principal) {
        Booking booking = findDetailedBooking(bookingId);
        authorizeBookingRead(booking, principal);
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        Invoice invoice = invoiceRepository.findByBookingId(bookingId).orElse(null);
        return BookingResponse.from(booking, payment, invoice);
    }

    @Transactional
    public BookingResponse checkIn(Integer bookingId, AppUserDetails principal) {
        Booking booking = findDetailedBooking(bookingId);
        authorizeGarageOperation(booking, principal);
        requireStatus(booking, BookingStatus.CONFIRMED, "Only CONFIRMED booking can be checked in");
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckinTime(OffsetDateTime.now());
        return responseWithPaymentAndInvoice(booking);
    }

    @Transactional
    public BookingResponse startWashing(Integer bookingId, AppUserDetails principal) {
        Booking booking = findDetailedBooking(bookingId);
        authorizeGarageOperation(booking, principal);
        requireStatus(booking, BookingStatus.CHECKED_IN, "Only CHECKED_IN booking can start washing");
        booking.setStatus(BookingStatus.WASHING);
        booking.setServiceStartTime(OffsetDateTime.now());
        return responseWithPaymentAndInvoice(booking);
    }

    @Transactional
    public BookingResponse complete(Integer bookingId, AppUserDetails principal) {
        Booking booking = findDetailedBooking(bookingId);
        authorizeGarageOperation(booking, principal);
        requireStatus(booking, BookingStatus.WASHING, "Only WASHING booking can be completed");
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Payment not found for booking"));
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking can only be completed after payment is PAID");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedTime(OffsetDateTime.now());
        Invoice invoice = invoiceRepository.findByBookingId(bookingId).orElse(null);
        return BookingResponse.from(booking, payment, invoice);
    }

    @Transactional
    public BookingResponse cancelBooking(Integer bookingId, AppUserDetails principal) {
        Booking booking = findDetailedBooking(bookingId);
        authorizeBookingCancel(booking, principal);
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PENDING or CONFIRMED booking can be cancelled");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT, "Paid booking must be refunded instead of cancelled");
        }

        OffsetDateTime now = OffsetDateTime.now();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);

        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setUpdatedAt(now);
            recordPaymentTransaction(payment, PaymentTransactionStatus.CANCELLED, "MANUAL", null);
        }

        Invoice invoice = invoiceRepository.findByBookingId(bookingId).orElse(null);
        return BookingResponse.from(booking, payment, invoice);
    }

    @Transactional
    public BookingResponse markNoShow(Integer bookingId, AppUserDetails principal) {
        Booking booking = findDetailedBooking(bookingId);
        authorizeGarageOperation(booking, principal);
        requireStatus(booking, BookingStatus.CONFIRMED, "Only CONFIRMED booking can be marked as NO_SHOW");

        booking.setStatus(BookingStatus.NO_SHOW);
        booking.setNoShowAt(OffsetDateTime.now());
        return responseWithPaymentAndInvoice(booking);
    }

    private void validateBookingInputs(BookingCreateRequest request, AppUser customer, Garage garage, BookingSlot slot, ServicePackage service, Vehicle vehicle) {
        if (garage.getStatus() != GarageStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Garage is not active");
        }
        if (slot.getStatus() != RecordStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking slot is not active");
        }
        if (service.getStatus() != RecordStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Service package is not active");
        }
        if (vehicle.getStatus() != RecordStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vehicle is not active");
        }
        if (!slot.getGarage().getId().equals(garage.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slot does not belong to garage");
        }
        if (!service.getGarage().getId().equals(garage.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Service package does not belong to garage");
        }
        if (!vehicle.getUser().getId().equals(customer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Vehicle does not belong to current user");
        }
        long activeBookings = bookingRepository.countActiveBookings(slot.getId(), garage.getId(), request.bookingDate(), CAPACITY_BLOCKING_STATUSES);
        if (activeBookings >= slot.getMaxCapacity()) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking slot is full");
        }
    }

    private Booking findDetailedBooking(Integer bookingId) {
        return bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private BookingResponse responseWithPaymentAndInvoice(Booking booking) {
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        Invoice invoice = invoiceRepository.findByBookingId(booking.getId()).orElse(null);
        return BookingResponse.from(booking, payment, invoice);
    }

    private void requireStatus(Booking booking, BookingStatus status, String message) {
        if (booking.getStatus() != status) {
            throw new ApiException(HttpStatus.CONFLICT, message);
        }
    }

    private void requireCustomer(AppUserDetails principal) {
        if (!principal.getRoleNames().contains("CUSTOMER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only CUSTOMER can create booking");
        }
    }

    private void authorizeBookingRead(Booking booking, AppUserDetails principal) {
        if (booking.getUser().getId().equals(principal.getId()) || canOperateGarage(booking, principal)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this booking");
    }

    private void authorizeGarageOperation(Booking booking, AppUserDetails principal) {
        if (!canOperateGarage(booking, principal)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot operate this booking");
        }
    }

    private void authorizeBookingCancel(Booking booking, AppUserDetails principal) {
        if (booking.getUser().getId().equals(principal.getId()) || canOperateGarage(booking, principal)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "You cannot cancel this booking");
    }

    private boolean canOperateGarage(Booking booking, AppUserDetails principal) {
        List<String> roles = principal.getRoleNames();
        if (roles.contains("ADMIN") || roles.contains("OWNER")) {
            return true;
        }
        return (roles.contains("STAFF") || roles.contains("MANAGER"))
                && principal.getGarageIds().contains(booking.getGarage().getId());
    }

    private String generateBookingCode() {
        return "BKG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private void recordPaymentTransaction(Payment payment, PaymentTransactionStatus status, String provider, String providerTxnId) {
        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .provider(provider)
                .providerTxnId(providerTxnId)
                .amount(payment.getAmount())
                .status(status)
                .build());
    }

}
