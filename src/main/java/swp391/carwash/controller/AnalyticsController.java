package swp391.carwash.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.dto.BookingBehaviorMonthlyResponse;
import swp391.carwash.dto.CustomerAiInsightResponse;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.AnalyticsService;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/api/admin/analytics/booking-behavior")
    public List<BookingBehaviorMonthlyResponse> getBookingBehavior(
            @RequestParam(required = false) Integer garageId,
            @RequestParam(required = false) String period) {
        return analyticsService.getBookingBehavior(garageId, period);
    }

    @GetMapping("/api/ai/insights/me")
    public List<CustomerAiInsightResponse> getMyInsights(
            @AuthenticationPrincipal AppUserDetails principal) {
        return analyticsService.getMyInsights(principal);
    }
}
