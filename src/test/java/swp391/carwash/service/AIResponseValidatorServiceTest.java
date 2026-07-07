package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIResponseValidatorServiceTest {
    private AIResponseValidatorService validatorService;

    @BeforeEach
    void setUp() {
        validatorService = new AIResponseValidatorService(new ObjectMapper());
    }

    @Test
    void parseAndValidateAcceptsValidInsightResponse() {
        String response = """
                {
                  "aiSummary": "Summary",
                  "aiExplanation": "Explanation",
                  "aiRecommendation": ["Action"],
                  "aiCampaignSuggestion": {
                    "campaignName": "Campaign",
                    "targetCustomers": "Returning customers",
                    "offer": "Discount",
                    "duration": "7 days",
                    "goal": "Increase completed bookings"
                  },
                  "confidenceScore": 0.82
                }
                """;

        var result = validatorService.parseAndValidate(response);

        assertEquals("Summary", result.getAiSummary());
        assertEquals(new BigDecimal("0.82"), result.getConfidenceScore());
    }

    @Test
    void parseAndValidateThrowsIllegalArgumentForInvalidInsightResponse() {
        String response = """
                {
                  "aiSummary": "",
                  "aiExplanation": "Explanation",
                  "aiRecommendation": ["Action"],
                  "aiCampaignSuggestion": {
                    "campaignName": "Campaign",
                    "targetCustomers": "Customers",
                    "offer": "Offer",
                    "duration": "7 days",
                    "goal": "Goal"
                  },
                  "confidenceScore": 0.5
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> validatorService.parseAndValidate(response));
    }

    @Test
    void parseAndValidateAcceptsJsonWrappedInMarkdownCodeBlock() {
        String response = """
                ```json
                {
                  "aiSummary": "Doanh thu giam do so don hoan tat thap.",
                  "aiExplanation": "Explanation",
                  "aiRecommendation": ["Kiem tra don huy"],
                  "aiCampaignSuggestion": {
                    "campaignName": "Campaign",
                    "targetCustomers": "Customers",
                    "offer": "Offer",
                    "duration": "7 days",
                    "goal": "Goal"
                  },
                  "confidenceScore": 0.76
                }
                ```
                """;

        var result = validatorService.parseAndValidate(response);

        assertEquals("Doanh thu giam do so don hoan tat thap.", result.getAiSummary());
        assertEquals(new BigDecimal("0.76"), result.getConfidenceScore());
    }

    @Test
    void parseAndValidateDetectedInsightsAcceptsStructuredEvidence() {
        String response = """
                {
                  "insights": [
                    {
                      "type": "REVENUE_DROP",
                      "severity": "WARNING",
                      "claim": "Revenue dropped",
                      "evidence": {
                        "metric": "revenue_change_percent",
                        "value": -23.00,
                        "period": "2026-07"
                      },
                      "suggested_action": "Review pricing"
                    }
                  ]
                }
                """;

        var result = validatorService.parseAndValidateDetectedInsights(response);

        assertEquals(1, result.insights().size());
        assertEquals("revenue_change_percent", result.insights().get(0).evidence().metric());
    }

    @Test
    void parseAndValidateDetectedInsightsRejectsMissingEvidence() {
        String response = """
                {
                  "insights": [
                    {
                      "type": "REVENUE_DROP",
                      "severity": "WARNING",
                      "claim": "Revenue dropped",
                      "suggested_action": "Review pricing"
                    }
                  ]
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> validatorService.parseAndValidateDetectedInsights(response));
    }
}
