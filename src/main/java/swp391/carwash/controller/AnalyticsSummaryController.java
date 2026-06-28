package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.dto.AnalyticsSummaryResponse;
import swp391.carwash.service.AnalyticsSummaryService;

@RestController
@RequiredArgsConstructor
public class AnalyticsSummaryController {
    private final AnalyticsSummaryService analyticsSummaryService;

    @GetMapping("/api/analytics/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public AnalyticsSummaryResponse getSummary() {
        return analyticsSummaryService.getSummary();
    }
}
