package swp391.carwash.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.BookingCreateRequest;
import swp391.carwash.dto.BookingResponse;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.BookingService;

@RestController
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @GetMapping("/api/bookings/me")
    public List<BookingResponse> getMyBookings(@AuthenticationPrincipal AppUserDetails principal) {
        return bookingService.getMyBookings(principal);
    }

    @PostMapping("/api/bookings")
    public BookingResponse createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return bookingService.createBooking(request, principal);
    }

    @GetMapping("/api/bookings/{id}")
    public BookingResponse getBooking(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return bookingService.getBooking(id, principal);
    }

    @PostMapping("/api/bookings/{id}/check-in")
    public BookingResponse checkIn(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return bookingService.checkIn(id, principal);
    }

    @PostMapping("/api/bookings/{id}/start-washing")
    public BookingResponse startWashing(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return bookingService.startWashing(id, principal);
    }

    @PostMapping("/api/bookings/{id}/complete")
    public BookingResponse complete(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return bookingService.complete(id, principal);
    }

    @PostMapping("/api/bookings/{id}/cancel")
    public BookingResponse cancelBooking(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return bookingService.cancelBooking(id, principal);
    }

    @PostMapping("/api/bookings/{id}/no-show")
    public BookingResponse markNoShow(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails principal) {
        return bookingService.markNoShow(id, principal);
    }
}
