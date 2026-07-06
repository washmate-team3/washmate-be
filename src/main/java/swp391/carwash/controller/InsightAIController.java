package swp391.carwash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.config.GeminiProperties;
import swp391.carwash.dto.insight.AIChatRequest;
import swp391.carwash.dto.insight.AIChatResponse;
import swp391.carwash.dto.insight.AIHealthResponse;
import swp391.carwash.dto.insight.AIInsightEnrichResponse;
import swp391.carwash.service.AIInsightChatService;
import swp391.carwash.service.AIInsightService;
import swp391.carwash.service.GeminiClient;

@RestController
@RequiredArgsConstructor
public class InsightAIController {
    private final AIInsightService aiInsightService;
    private final AIInsightChatService aiInsightChatService;
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

    @PostMapping({
            "/api/owner/insights/ai-chat",
            "/api/owner/insights/ai/chat",
            "/api/owner/insights/chat",
            "/api/owner/ai-chat"
    })
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public AIChatResponse chat(@Valid @RequestBody AIChatRequest request) {
        return aiInsightChatService.chat(request);
    }

    @GetMapping({
            "/api/owner/insights/ai-health",
            "/api/owner/insights/ai/health",
            "/api/owner/ai-health"
    })
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
