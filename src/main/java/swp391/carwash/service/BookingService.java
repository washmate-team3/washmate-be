package swp391.carwash.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.BookingCreateRequest;
import swp391.carwash.dto.BookingRejectRequest;
import swp391.carwash.dto.BookingResponse;
import swp391.carwash.dto.BookingUpdateRequest;
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
            BookingStatus.WASHING);

    private final AppUserRepository appUserRepository;
    private final BookingRepository bookingRepository;
    private final BookingSlotRepository bookingSlotRepository;
    private final GarageRepository garageRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final VehicleRepository vehicleRepository;
    private final LoyaltyService loyaltyService;
    private final PromotionRepository promotionRepository;
    private final NotificationRepository notificationRepository;
    private final PromotionUsageRepository promotionUsageRepository;

    @Value("${washmate.payment.vnpay.timeout-minutes:15}")
    private int paymentTimeoutMinutes;

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

        BigDecimal discountAmount = calculateDiscount(
                customer.getId(),
                garage,
                service,
                request.promotionId());

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

        if (request.promotionId() != null) {

            Promotion promotion = promotionRepository.findById(request.promotionId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "Mã khuyến mãi không tồn tại"));

            promotionUsageRepository.save(
                    PromotionUsage.builder()
                            .promotion(promotion)
                            .user(customer)
                            .booking(booking)
                            .usedAt(OffsetDateTime.now())
                            .build()

            );
            promotion.setUsedCount(promotion.getUsedCount() + 1);

            promotionRepository.save(promotion);
        }


        //notification khi đặt lịch chờ xác nhận
        notificationRepository.save(Notification.builder()
                .userId(customer.getId())
                .bookingId(booking.getId())
                .title("Đặt lịch thành công")
                .content(String.format("Yêu cầu đặt lịch %s tại %s đang chờ xác nhận.", booking.getBookingCode(), garage.getName()))
                .type("BOOKING_CONFIRMATION")
                .channel("IN_APP")
                .status("PENDING")
                .build());

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseGet(() -> paymentRepository.save(Payment.builder()
                        .booking(booking)
                        .garage(garage)
                        .amount(finalAmount)
                        .method(request.paymentMethod() == null ? PaymentMethod.CASH : request.paymentMethod())
                        .status(PaymentStatus.PENDING)
                        .expiresAt(OffsetDateTime.now().plusMinutes(paymentTimeoutMinutes))
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
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(principal.getId());
        if (bookings.isEmpty()) {
            return List.of();
        }

        List<Integer> bookingIds = bookings.stream().map(Booking::getId).toList();

        Map<Integer, Payment> paymentMap = paymentRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(p -> p.getBooking().getId(), p -> p, (p1, p2) -> p1));

        Map<Integer, Invoice> invoiceMap = invoiceRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(i -> i.getBooking().getId(), i -> i, (i1, i2) -> i1));

        return bookings.stream()
                .map(booking -> BookingResponse.from(
                        booking,
                        paymentMap.get(booking.getId()),
                        invoiceMap.get(booking.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookings(BookingStatus status, Integer garageId, LocalDate fromDate, LocalDate toDate, Pageable pageable, AppUserDetails principal) {
        List<Integer> garageIds = null;
        List<String> roles = principal.getRoleNames();
        if (!roles.contains("ADMIN") && !roles.contains("OWNER")) {
            garageIds = principal.getGarageIds();
            if (garageIds.isEmpty()) {
                return Page.empty(pageable);
            }
        }

        Page<Booking> bookingPage = bookingRepository.findBookingsWithFilters(status, garageId, fromDate, toDate, garageIds, pageable);
        if (bookingPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Integer> bookingIds = bookingPage.getContent().stream().map(Booking::getId).toList();

        Map<Integer, Payment> paymentMap = paymentRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(p -> p.getBooking().getId(), p -> p, (p1, p2) -> p1));

        Map<Integer, Invoice> invoiceMap = invoiceRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(i -> i.getBooking().getId(), i -> i, (i1, i2) -> i1));

        return bookingPage.map(booking -> BookingResponse.from(
                booking,
                paymentMap.get(booking.getId()),
                invoiceMap.get(booking.getId())));
    }

    @Transactional
    public BookingResponse updateBooking(Integer bookingId, BookingUpdateRequest request, AppUserDetails principal) {
        Booking booking = findDetailedBooking(bookingId);

        boolean isOwner = booking.getUser().getId().equals(principal.getId());
        boolean isStaff = canOperateGarage(booking, principal);
        if (!isOwner && !isStaff) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot update this booking");
        }

        if (!isOwner && isStaff && !booking.getGarage().getId().equals(request.garageId())) {
            List<String> roles = principal.getRoleNames();
            if (!roles.contains("ADMIN") && !roles.contains("OWNER")) {
                if (!principal.getGarageIds().contains(request.garageId())) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "You cannot transfer booking to a garage you don't manage");
                }
            }
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PENDING or CONFIRMED booking can be updated");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot update booking because payment is not PENDING");
        }

        Garage newGarage = booking.getGarage().getId().equals(request.garageId()) ? booking.getGarage() :
                garageRepository.findById(request.garageId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Garage not found"));
        BookingSlot newSlot = booking.getSlot().getId().equals(request.slotId()) ? booking.getSlot() :
                bookingSlotRepository.findByIdForUpdate(request.slotId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking slot not found"));
        ServicePackage newService = booking.getService().getId().equals(request.serviceId()) ? booking.getService() :
                servicePackageRepository.findById(request.serviceId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Service package not found"));
        Vehicle newVehicle = booking.getVehicle().getId().equals(request.vehicleId()) ? booking.getVehicle() :
                vehicleRepository.findById(request.vehicleId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        if (newGarage.getStatus() != GarageStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New garage is not active");
        }
        if (newSlot.getStatus() != RecordStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New slot is not active");
        }
        if (newService.getStatus() != RecordStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New service is not active");
        }
        if (newVehicle.getStatus() != RecordStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New vehicle is not active");
        }
        if (!newSlot.getGarage().getId().equals(newGarage.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slot does not belong to garage");
        }
        if (!newService.getGarage().getId().equals(newGarage.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Service does not belong to garage");
        }
        if (!newVehicle.getUser().getId().equals(booking.getUser().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vehicle does not belong to the customer");
        }

        long activeBookings = bookingRepository.countActiveBookingsForUpdate(newSlot.getId(), newGarage.getId(), request.bookingDate(), CAPACITY_BLOCKING_STATUSES, booking.getId());
        if (activeBookings >= newSlot.getMaxCapacity()) {
            throw new ApiException(HttpStatus.CONFLICT, "New booking slot is full");
        }

        booking.setGarage(newGarage);
        booking.setSlot(newSlot);
        booking.setService(newService);
        booking.setVehicle(newVehicle);
        booking.setBookingDate(request.bookingDate());

        BigDecimal newTotal = newService.getPrice();
        BigDecimal newFinal = newTotal.subtract(booking.getDiscountAmount());
        booking.setTotalAmount(newTotal);
        booking.setFinalAmount(newFinal);

        payment.setAmount(newFinal);
        payment.setGarage(newGarage);
        payment.setUpdatedAt(OffsetDateTime.now());

        notificationRepository.save(Notification.builder()
                .userId(booking.getUser().getId())
                .bookingId(booking.getId())
                .title("cập nhập thành công")
                .content(String.format("Đơn đặt lịch %s của bạn đã được hủy bỏ thành công.", booking.getBookingCode()))
                .type("BOOKING_CONFIRMATION")
                .channel("IN_APP")
                .status("PENDING")
                .build());

        return BookingResponse.from(booking, payment, invoiceRepository.findByBookingId(bookingId).orElse(null));
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
    public BookingResponse confirmBooking(Integer bookingId, AppUserDetails principal) {
        Booking booking = findDetailedBooking(bookingId);
        authorizeGarageOperation(booking, principal);
        requireStatus(booking, BookingStatus.PENDING, "Only PENDING booking can be confirmed");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(OffsetDateTime.now());

        notificationRepository.save(Notification.builder()
                .userId(booking.getUser().getId())
                .bookingId(booking.getId())
                .title("Đơn đặt lịch được xác nhận")
                .content(String.format("Lịch rửa xe %s đã được xác nhận thành công bởi %s.", booking.getBookingCode(), booking.getGarage().getName()))
                .type("BOOKING_CONFIRMATION")
                .channel("IN_APP")
                .status("PENDING")
                .build());

        return responseWithPaymentAndInvoice(booking);

    }

    @Transactional
    public BookingResponse rejectBooking(Integer bookingId, BookingRejectRequest request, AppUserDetails principal) {
        Booking booking = findDetailedBooking(bookingId);
        authorizeGarageOperation(booking, principal);
        requireStatus(booking, BookingStatus.PENDING, "Only PENDING booking can be rejected");

        OffsetDateTime now = OffsetDateTime.now();
        booking.setStatus(BookingStatus.REJECTED);
        booking.setCancelledAt(now);
        booking.setRejectionReason(request.reason());

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setUpdatedAt(now);
            recordPaymentTransaction(payment, PaymentTransactionStatus.CANCELLED, "MANUAL", null);
        }
        notificationRepository.save(Notification.builder()
                .userId(booking.getUser().getId())
                .bookingId(booking.getId())
                .title("Đơn đặt lịch bị từ chối")
                .content(String.format("Rất tiếc, đơn đặt lịch %s đã bị từ chối. Lý do: %s", booking.getBookingCode(), request.reason()))
                .type("BOOKING_CONFIRMATION")
                .channel("IN_APP")
                .status("PENDING")
                .build());

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

        notificationRepository.save(Notification.builder()
                .userId(booking.getUser().getId())
                .bookingId(booking.getId())
                .title("Xe của bạn đang được rửa")
                .content(String.format("Garage %s đã bắt đầu tiến hành dịch vụ rửa xe cho mã đơn %s.", booking.getGarage().getName(), booking.getBookingCode()))
                .type("BOOKING_CONFIRMATION")
                .channel("IN_APP")
                .status("PENDING")
                .build());

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

        loyaltyService.accruePoints(booking);

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

        notificationRepository.save(Notification.builder()
                .userId(booking.getUser().getId())
                .bookingId(booking.getId())
                .title("Hủy lịch thành công")
                .content(String.format("Đơn đặt lịch %s của bạn đã được hủy bỏ thành công.", booking.getBookingCode()))
                .type("BOOKING_CONFIRMATION")
                .channel("IN_APP")
                .status("PENDING")
                .build());

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

    private void validateBookingInputs(BookingCreateRequest request, AppUser customer, Garage garage, BookingSlot slot,
            ServicePackage service, Vehicle vehicle) {
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
        long activeBookings = bookingRepository.countActiveBookings(slot.getId(), garage.getId(), request.bookingDate(),
                CAPACITY_BLOCKING_STATUSES);
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

    private void recordPaymentTransaction(Payment payment, PaymentTransactionStatus status, String provider,
            String providerTxnId) {
        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .provider(provider)
                .providerTxnId(providerTxnId)
                .amount(payment.getAmount())
                .status(status)
                .build());
    }
    private BigDecimal calculateDiscount(
            Integer customerId,
            Garage garage,
            ServicePackage service,
            Integer promotionId) {

        BigDecimal totalAmount = service.getPrice();
        BigDecimal discount = BigDecimal.ZERO;

        if (promotionId == null) {
            return discount;
        }

        OffsetDateTime now = OffsetDateTime.now();

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Mã khuyến mãi không tồn tại"));
        if (promotionUsageRepository.existsByUser_IdAndPromotion_PromotionId(
                customerId,
                promotion.getPromotionId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Bạn đã sử dụng mã khuyến mãi này.");
        }

        if (!"ACTIVE".equals(promotion.getStatus())
                || now.isBefore(promotion.getStartDate())
                || now.isAfter(promotion.getEndDate())
                || (promotion.getUsageLimit() != null
                && promotion.getUsedCount() >= promotion.getUsageLimit())
                || totalAmount.compareTo(promotion.getMinOrderValue()) < 0) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mã khuyến mãi không hợp lệ hoặc đã hết hạn");
        }

        if (!promotion.getGarageId().equals(garage.getId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mã khuyến mãi không áp dụng cho garage này");
        }

        if ("PERCENTAGE".equals(promotion.getDiscountType())) {

            discount = totalAmount.multiply(
                            promotion.getDiscountValue())
                    .divide(new BigDecimal("100"));

            if (promotion.getMaxDiscount() != null
                    && discount.compareTo(promotion.getMaxDiscount()) > 0) {

                discount = promotion.getMaxDiscount();
            }

        } else if ("FIXED_AMOUNT".equals(promotion.getDiscountType())) {

            discount = promotion.getDiscountValue();
        }

        if (discount.compareTo(totalAmount) > 0) {
            discount = totalAmount;
        }

        return discount;
    }

}
