package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.config.GeminiProperties;
import swp391.carwash.dto.insight.AIDeepAnalysisRequest;
import swp391.carwash.dto.insight.AIDeepAnalysisResponse;
import swp391.carwash.dto.insight.AIHealthResponse;
import swp391.carwash.dto.insight.AIInsightEnrichResponse;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.AIDeepAnalysisService;
import swp391.carwash.service.AIInsightService;
import swp391.carwash.service.GeminiClient;

@RestController
@RequiredArgsConstructor
public class InsightAIController {
    private final AIInsightService aiInsightService;
    private final AIDeepAnalysisService aiDeepAnalysisService;
    private final GeminiClient geminiClient;
    private final GeminiProperties geminiProperties;

    @PostMapping("/api/owner/insights/{id}/ai-enrich")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public AIInsightEnrichResponse enrichInsight(@PathVariable Integer id) {
        return aiInsightService.enrichInsight(id);
    }

    @PostMapping("/api/owner/insights/{id}/ai-regenerate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public AIInsightEnrichResponse regenerateInsight(@PathVariable Integer id) {
        return aiInsightService.regenerateInsight(id);
    }

    @PostMapping("/api/owner/insights/deep-analysis")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public AIDeepAnalysisResponse deepAnalysis(
            @RequestBody(required = false) AIDeepAnalysisRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return aiDeepAnalysisService.analyze(request, principal);
    }

    @GetMapping("/api/owner/insights/ai-health")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public AIHealthResponse health() {
        boolean configured = geminiClient.isConfigured();
        return new AIHealthResponse(
                configured,
                geminiProperties.getModel(),
                geminiProperties.getPrompt().getVersion(),
                configured ? "AI service is configured" : "Gemini API key is not configured");
    }
}
