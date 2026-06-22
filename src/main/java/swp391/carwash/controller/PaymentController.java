package swp391.carwash.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.dto.BookingResponse;
import swp391.carwash.dto.PaymentActionRequest;
import swp391.carwash.dto.PaymentConfirmRequest;
import swp391.carwash.dto.PaymentResponse;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.PaymentService;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/api/payments/{id}")
    public PaymentResponse getPayment(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return paymentService.getPayment(id, principal);
    }

    @GetMapping("/api/bookings/{bookingId}/payment")
    public PaymentResponse getPaymentByBooking(
            @PathVariable Integer bookingId,
            @AuthenticationPrincipal AppUserDetails principal) {
        return paymentService.getPaymentByBooking(bookingId, principal);
    }

    @GetMapping("/api/payments/{id}/transactions")
    public List<PaymentResponse.TransactionInfo> getPaymentTransactions(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return paymentService.getPaymentTransactions(id, principal);
    }

    @PostMapping("/api/payments/{id}/confirm")
    public BookingResponse confirmPayment(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) PaymentConfirmRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return paymentService.confirmPayment(id, request, principal);
    }

    @PostMapping("/api/payments/{id}/fail")
    public BookingResponse failPayment(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) PaymentActionRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return paymentService.failPayment(id, request, principal);
    }

    @PostMapping("/api/payments/{id}/cancel")
    public BookingResponse cancelPayment(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) PaymentActionRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return paymentService.cancelPayment(id, request, principal);
    }

    @PostMapping("/api/payments/{id}/refund")
    public BookingResponse refundPayment(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) PaymentActionRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return paymentService.refundPayment(id, request, principal);
    }
}
