package swp391.carwash.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.dto.CustomerAiInsightAdminResponse;
import swp391.carwash.service.AiInsightGenerationService;

@RestController
@RequiredArgsConstructor
public class AdminAiInsightController {
    private final AiInsightGenerationService aiInsightGenerationService;

    @PostMapping("/api/admin/insights/generate")
    public List<CustomerAiInsightAdminResponse> generateInsights(
            @RequestParam Integer garageId,
            @RequestParam String period) {
        return aiInsightGenerationService.generateInsights(garageId, period);
    }

    @GetMapping("/api/admin/insights")
    public List<CustomerAiInsightAdminResponse> getInsights(
            @RequestParam Integer garageId,
            @RequestParam String period) {
        return aiInsightGenerationService.getInsights(garageId, period);
    }
}
