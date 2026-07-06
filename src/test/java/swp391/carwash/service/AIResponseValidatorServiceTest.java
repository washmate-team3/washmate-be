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
    void parseAndValidateChatAcceptsJsonCodeBlock() {
        String response = """
                ```json
                {
                  "answer": "Doanh thu giam do so don hoan tat thap.",
                  "suggestedActions": ["Kiem tra don huy"],
                  "referencedInsightIds": [1],
                  "confidenceScore": 0.76
                }
                ```
                """;

        var result = validatorService.parseAndValidateChat(response);

        assertEquals("Doanh thu giam do so don hoan tat thap.", result.getAnswer());
        assertEquals(1, result.getReferencedInsightIds().getFirst());
    }
}
