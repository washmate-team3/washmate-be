package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import swp391.carwash.config.GeminiProperties;
import swp391.carwash.entity.BusinessInsight;
import swp391.carwash.entity.InsightAIEnrichment;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightStatus;
import swp391.carwash.enums.InsightType;
import swp391.carwash.repository.BusinessInsightRepository;
import swp391.carwash.repository.InsightAIEnrichmentRepository;
import swp391.carwash.service.insight.rule.RevenueInsightRule;

@ExtendWith(MockitoExtension.class)
class AIInsightServiceTest {
    @Mock
    private BusinessInsightRepository businessInsightRepository;
    @Mock
    private InsightAIEnrichmentRepository aiEnrichmentRepository;
    @Mock
    private AIPromptBuilderService promptBuilderService;
    @Mock
    private GeminiClient geminiClient;
    @Mock
    private AIResponseValidatorService validatorService;

    private AIInsightService aiInsightService;

    @BeforeEach
    void setUp() {
        aiInsightService = new AIInsightService(
                businessInsightRepository,
                aiEnrichmentRepository,
                promptBuilderService,
                geminiClient,
                validatorService,
                new GeminiProperties(),
                new ObjectMapper());
    }

    @Test
    void enrichInsightReturnsExistingEnrichmentWithoutCallingGemini() {
        BusinessInsight insight = validInsight();
        InsightAIEnrichment enrichment = existingEnrichment(insight);
        when(businessInsightRepository.findById(1)).thenReturn(Optional.of(insight));
        when(aiEnrichmentRepository.findByBusinessInsightId(1)).thenReturn(Optional.of(enrichment));

        var response = aiInsightService.enrichInsight(1);

        assertEquals("Summary", response.getAiSummary());
        verifyNoInteractions(geminiClient);
    }

    @Test
    void enrichInsightMapsInvalidGeminiResponseToUnprocessableEntity() {
        BusinessInsight insight = validInsight();
        when(businessInsightRepository.findById(1)).thenReturn(Optional.of(insight));
        when(aiEnrichmentRepository.findByBusinessInsightId(1)).thenReturn(Optional.empty());
        when(promptBuilderService.buildInsightContext(insight)).thenReturn("{}");
        when(promptBuilderService.buildPrompt("{}")).thenReturn("prompt");
        when(geminiClient.generateContent("prompt")).thenReturn("{}");
        when(validatorService.parseAndValidate(anyString())).thenThrow(new IllegalArgumentException("bad format"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> aiInsightService.enrichInsight(1));

        assertEquals(HttpStatus.valueOf(422), exception.getStatusCode());
    }

    private BusinessInsight validInsight() {
        return BusinessInsight.builder()
                .id(1)
                .ruleCode(RevenueInsightRule.REVENUE_GROWTH)
                .type(InsightType.REVENUE)
                .severity(InsightSeverity.POSITIVE)
                .title("Revenue growth")
                .summary("Revenue increased.")
                .evidence("Current revenue is higher than previous period.")
                .meaning("Positive trend.")
                .recommendation("Keep monitoring services.")
                .fromDate(LocalDate.of(2026, 7, 1))
                .toDate(LocalDate.of(2026, 7, 31))
                .status(InsightStatus.NEW)
                .createdAt(OffsetDateTime.parse("2026-07-31T10:00:00+07:00"))
                .build();
    }

    private InsightAIEnrichment existingEnrichment(BusinessInsight insight) {
        return InsightAIEnrichment.builder()
                .id(10)
                .businessInsight(insight)
                .aiSummary("Summary")
                .aiExplanation("Explanation")
                .aiRecommendation("[\"Action\"]")
                .aiCampaignSuggestion("""
                        {
                          "campaignName": "Campaign",
                          "targetCustomers": "Customers",
                          "offer": "Offer",
                          "duration": "7 days",
                          "goal": "Goal"
                        }
                        """)
                .confidenceScore(new BigDecimal("0.80"))
                .aiModel("gemini-2.5-flash")
                .promptVersion("v1")
                .generatedAt(OffsetDateTime.parse("2026-07-31T10:00:00+07:00"))
                .build();
    }
}
