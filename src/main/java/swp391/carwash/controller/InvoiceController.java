package swp391.carwash.controller;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.dto.InvoiceResponse;
import swp391.carwash.enums.InvoiceStatus;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.InvoiceService;

@RestController
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @GetMapping("/api/invoices/{id}")
    public InvoiceResponse getInvoice(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return invoiceService.getInvoice(id, principal);
    }

    @GetMapping("/api/bookings/{bookingId}/invoice")
    public InvoiceResponse getInvoiceByBooking(
            @PathVariable Integer bookingId,
            @AuthenticationPrincipal AppUserDetails principal) {
        return invoiceService.getInvoiceByBooking(bookingId, principal);
    }

    @GetMapping("/api/admin/invoices")
    public Page<InvoiceResponse> getAdminInvoices(
            @RequestParam(required = false) Integer garageId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return invoiceService.getAdminInvoices(garageId, status, fromDate, toDate, pageable);
    }
}
