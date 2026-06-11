package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.dto.InvoiceResponse;
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
}
