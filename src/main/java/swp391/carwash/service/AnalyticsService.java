package swp391.carwash.service;

import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.BookingBehaviorMonthlyResponse;
import swp391.carwash.dto.CustomerAiInsightResponse;
import swp391.carwash.repository.BookingRepository;
import swp391.carwash.repository.CustomerAiInsightRepository;
import swp391.carwash.security.AppUserDetails;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final BookingRepository bookingRepository;
    private final CustomerAiInsightRepository customerAiInsightRepository;

    @Transactional(readOnly = true)
    public List<BookingBehaviorMonthlyResponse> getBookingBehavior(Integer garageId, String period) {
        String normalizedPeriod = normalizePeriod(period);
        return bookingRepository.findBookingBehaviorMonthly(garageId, normalizedPeriod).stream()
                .map(BookingBehaviorMonthlyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerAiInsightResponse> getMyInsights(AppUserDetails principal) {
        return customerAiInsightRepository.findByUserIdOrderByGeneratedAtDesc(principal.getId()).stream()
                .map(CustomerAiInsightResponse::from)
                .toList();
    }

    private String normalizePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            return null;
        }
        String normalized = period.trim();
        if (!PERIOD_PATTERN.matcher(normalized).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Period must use YYYY-MM format");
        }
        return normalized;
    }
}
