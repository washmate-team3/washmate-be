package swp391.carwash.Schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import swp391.carwash.service.LoyaltyTierEvaluationService;

@Component
@RequiredArgsConstructor
public class LoyaltyTierScheduler {

    private final LoyaltyTierEvaluationService loyaltyTierEvaluationService;

    @Scheduled(cron = "0 0 0 1 1,4,7,10 *")
    public void evaluateTier() {

        loyaltyTierEvaluationService.evaluateQuarterlyTier();

    }
    @Scheduled(cron = "0 0 8 * * *")
    public void notifyMaintainWarning() {

        loyaltyTierEvaluationService.notifyMaintainWarning();

    }


}
