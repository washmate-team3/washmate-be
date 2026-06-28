package swp391.carwash.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.InvoiceResponse;
import swp391.carwash.entity.Booking;
import swp391.carwash.entity.Invoice;
import swp391.carwash.enums.InvoiceStatus;
import swp391.carwash.repository.BookingRepository;
import swp391.carwash.repository.InvoiceRepository;
import swp391.carwash.security.AppUserDetails;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Integer invoiceId, AppUserDetails principal) {
        Invoice invoice = invoiceRepository.findDetailedById(invoiceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));
        authorizeBookingRead(invoice.getBooking(), principal);
        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByBooking(Integer bookingId, AppUserDetails principal) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        authorizeBookingRead(booking, principal);
        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));
        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAdminInvoices(
            Integer garageId,
            InvoiceStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        OffsetDateTime fromTime = startOfDay(fromDate);
        OffsetDateTime toTime = toDate == null ? null : startOfDay(toDate.plusDays(1));
        return invoiceRepository.findAdminInvoices(garageId, status, fromTime, toTime, pageable)
                .map(InvoiceResponse::from);
    }

    private void authorizeBookingRead(Booking booking, AppUserDetails principal) {
        if (booking.getUser().getId().equals(principal.getId()) || canOperateGarage(booking, principal)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this invoice");
    }

    private boolean canOperateGarage(Booking booking, AppUserDetails principal) {
        List<String> roles = principal.getRoleNames();
        if (roles.contains("ADMIN") || roles.contains("OWNER")) {
            return true;
        }
        return (roles.contains("STAFF") || roles.contains("MANAGER"))
                && principal.getGarageIds().contains(booking.getGarage().getId());
    }

    private OffsetDateTime startOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
