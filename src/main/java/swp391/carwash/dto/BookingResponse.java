package swp391.carwash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.Invoice;
import swp391.carwash.entity.Payment;

public record BookingResponse(
        Integer id,
        String bookingCode,
        String status,
        LocalDate bookingDate,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        OffsetDateTime confirmedAt,
        OffsetDateTime checkinTime,
        OffsetDateTime serviceStartTime,
        OffsetDateTime completedTime,
        UserInfo customer,
        GarageInfo garage,
        SlotInfo slot,
        ServiceInfo service,
        VehicleInfo vehicle,
        PaymentInfo payment,
        InvoiceInfo invoice
) {
    public static BookingResponse from(Booking booking, Payment payment, Invoice invoice) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getStatus().name(),
                booking.getBookingDate(),
                booking.getTotalAmount(),
                booking.getDiscountAmount(),
                booking.getFinalAmount(),
                booking.getConfirmedAt(),
                booking.getCheckinTime(),
                booking.getServiceStartTime(),
                booking.getCompletedTime(),
                new UserInfo(booking.getUser().getId(), booking.getUser().getFullName(), booking.getUser().getEmail(), booking.getUser().getPhone()),
                new GarageInfo(booking.getGarage().getId(), booking.getGarage().getName()),
                new SlotInfo(booking.getSlot().getId(), booking.getSlot().getStartTime(), booking.getSlot().getEndTime()),
                new ServiceInfo(booking.getService().getId(), booking.getService().getName(), booking.getService().getPrice(), booking.getService().getDuration()),
                new VehicleInfo(booking.getVehicle().getId(), booking.getVehicle().getLicensePlate(), booking.getVehicle().getBrand(), booking.getVehicle().getModel()),
                payment == null ? null : new PaymentInfo(payment.getId(), payment.getAmount(), payment.getMethod().name(), payment.getStatus().name(), payment.getPaidAt()),
                invoice == null ? null : new InvoiceInfo(invoice.getId(), invoice.getInvoiceCode(), invoice.getTotalAmount(), invoice.getStatus().name(), invoice.getIssuedAt(), invoice.getPaidAt())
        );
    }

    public record UserInfo(Integer id, String fullName, String email, String phone) {
    }

    public record GarageInfo(Integer id, String name) {
    }

    public record SlotInfo(Integer id, LocalTime startTime, LocalTime endTime) {
    }

    public record ServiceInfo(Integer id, String name, BigDecimal price, Integer duration) {
    }

    public record VehicleInfo(Integer id, String licensePlate, String brand, String model) {
    }

    public record PaymentInfo(Integer id, BigDecimal amount, String method, String status, OffsetDateTime paidAt) {
    }

    public record InvoiceInfo(Integer id, String invoiceCode, BigDecimal totalAmount, String status, OffsetDateTime issuedAt, OffsetDateTime paidAt) {
    }
}
