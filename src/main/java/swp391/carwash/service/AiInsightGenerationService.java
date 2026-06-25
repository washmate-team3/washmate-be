package swp391.carwash.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.CustomerAiInsightAdminResponse;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.CustomerAiInsight;
import swp391.carwash.entity.Garage;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.BookingBehaviorMonthlyView;
import swp391.carwash.repository.BookingRepository;
import swp391.carwash.repository.CustomerAiInsightRepository;
import swp391.carwash.repository.GarageRepository;

@Service
@RequiredArgsConstructor
public class AiInsightGenerationService {
    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final BookingRepository bookingRepository;
    private final CustomerAiInsightRepository customerAiInsightRepository;
    private final AppUserRepository appUserRepository;
    private final GarageRepository garageRepository;
    private final AiInsightRuleEngine ruleEngine;

    @Transactional
    public List<CustomerAiInsightAdminResponse> generateInsights(Integer garageId, String period) {
        String normalizedPeriod = normalizePeriod(period);
        validateGarageId(garageId);

        List<BookingBehaviorMonthlyView> behaviors = bookingRepository.findBookingBehaviorMonthly(garageId, normalizedPeriod);
        OffsetDateTime now = OffsetDateTime.now();
        for (BookingBehaviorMonthlyView behavior : behaviors) {
            AppUser user = appUserRepository.getReferenceById(behavior.getUserId());
            Garage garage = garageRepository.getReferenceById(behavior.getGarageId());
            for (AiInsightRuleEngine.InsightDraft draft : ruleEngine.evaluate(behavior)) {
                CustomerAiInsight insight = customerAiInsightRepository
                        .findByUserIdAndGarageIdAndPeriodAndInsightTypeAndModelVersion(
                                behavior.getUserId(),
                                behavior.getGarageId(),
                                behavior.getMonthYear(),
                                draft.insightType(),
                                AiInsightRuleEngine.MODEL_VERSION)
                        .orElseGet(CustomerAiInsight::new);
                insight.setUser(user);
                insight.setGarage(garage);
                insight.setPeriod(behavior.getMonthYear());
                insight.setInsightType(draft.insightType());
                insight.setPredictionValue(draft.predictionValue());
                insight.setConfidenceScore(draft.confidenceScore());
                insight.setModelVersion(AiInsightRuleEngine.MODEL_VERSION);
                insight.setGeneratedAt(now);
                customerAiInsightRepository.save(insight);
            }
        }
        customerAiInsightRepository.flush();
        return getInsights(garageId, normalizedPeriod);
    }

    @Transactional(readOnly = true)
    public List<CustomerAiInsightAdminResponse> getInsights(Integer garageId, String period) {
        String normalizedPeriod = normalizePeriod(period);
        validateGarageId(garageId);
        return customerAiInsightRepository.findByGarageIdAndPeriodOrderByGeneratedAtDesc(garageId, normalizedPeriod)
                .stream()
                .map(CustomerAiInsightAdminResponse::from)
                .toList();
    }

    private void validateGarageId(Integer garageId) {
        if (garageId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Garage id is required");
        }
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Period is required");
        }
        String normalized = period.trim();
        if (!PERIOD_PATTERN.matcher(normalized).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Period must use YYYY-MM format");
        }
        return normalized;
    }
}
