package swp391.carwash.testutil;

import java.math.BigDecimal;
import java.time.LocalDate;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.BookingSlot;
import swp391.carwash.entity.Garage;
import swp391.carwash.entity.Payment;
import swp391.carwash.entity.ServicePackage;
import swp391.carwash.entity.Vehicle;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.enums.PaymentMethod;
import swp391.carwash.enums.PaymentStatus;

/**
 * Factory dữ liệu test dùng chung — tránh lặp lại builder entity ở mỗi file test.
 * Quy ước id cố định: garage=1, customer=10, vehicle=20, slot=30, service=40,
 * booking=100, payment=200.
 */
public final class TestData {
    public static final BigDecimal DEFAULT_PRICE = new BigDecimal("50000.00");

    private TestData() {
    }

    public static Garage garage() {
        return Garage.builder().id(1).name("Garage 1").address("Address").phone("0900000000").build();
    }

    public static AppUser customer() {
        return AppUser.builder().id(10).fullName("Customer").phone("0911111111").build();
    }

    public static Vehicle vehicle(AppUser owner) {
        return Vehicle.builder().id(20).user(owner).licensePlate("59A1-12345").build();
    }

    public static BookingSlot slot(Garage garage) {
        return BookingSlot.builder().id(30).garage(garage).build();
    }

    public static ServicePackage servicePackage(Garage garage) {
        return ServicePackage.builder()
                .id(40)
                .garage(garage)
                .name("Basic Wash")
                .price(DEFAULT_PRICE)
                .duration(30)
                .build();
    }

    /** Booking PENDING (id=100) với đầy đủ quan hệ: customer 10, garage 1, giá 50.000. */
    public static Booking pendingBooking(String bookingCode) {
        Garage garage = garage();
        AppUser customer = customer();
        return Booking.builder()
                .id(100)
                .bookingCode(bookingCode)
                .user(customer)
                .garage(garage)
                .slot(slot(garage))
                .service(servicePackage(garage))
                .vehicle(vehicle(customer))
                .bookingDate(LocalDate.now())
                .totalAmount(DEFAULT_PRICE)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(DEFAULT_PRICE)
                .status(BookingStatus.PENDING)
                .build();
    }

    /** Payment PENDING (id=200) gắn với booking, amount = finalAmount của booking. */
    public static Payment pendingPayment(Booking booking, PaymentMethod method) {
        return Payment.builder()
                .id(200)
                .booking(booking)
                .garage(booking.getGarage())
                .amount(booking.getFinalAmount())
                .method(method)
                .status(PaymentStatus.PENDING)
                .build();
    }
}
