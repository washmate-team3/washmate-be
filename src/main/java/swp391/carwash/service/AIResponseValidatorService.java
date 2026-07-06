package swp391.carwash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import swp391.carwash.dto.insight.AIChatResult;
import swp391.carwash.dto.insight.AIInsightResult;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIResponseValidatorService {
    private final ObjectMapper objectMapper;

    public AIInsightResult parseAndValidate(String rawGeminiResponse) {
        try {
            String jsonContent = extractJsonContent(rawGeminiResponse);
            AIInsightResult result = objectMapper.readValue(jsonContent.trim(), AIInsightResult.class);

            if (!StringUtils.hasText(result.getAiSummary())) {
                throw new IllegalArgumentException("aiSummary must not be empty");
            }
            if (!StringUtils.hasText(result.getAiExplanation())) {
                throw new IllegalArgumentException("aiExplanation must not be empty");
            }
            if (result.getAiRecommendation() == null || result.getAiRecommendation().isEmpty()) {
                throw new IllegalArgumentException("aiRecommendation must have at least 1 item");
            }

            if (result.getAiCampaignSuggestion() != null) {
                if (!StringUtils.hasText(result.getAiCampaignSuggestion().getCampaignName()) ||
                    !StringUtils.hasText(result.getAiCampaignSuggestion().getTargetCustomers()) ||
                    !StringUtils.hasText(result.getAiCampaignSuggestion().getOffer()) ||
                    !StringUtils.hasText(result.getAiCampaignSuggestion().getDuration()) ||
                    !StringUtils.hasText(result.getAiCampaignSuggestion().getGoal())) {
                    throw new IllegalArgumentException("aiCampaignSuggestion is missing required fields");
                }
            } else {
                throw new IllegalArgumentException("aiCampaignSuggestion must not be null");
            }

            if (result.getConfidenceScore() == null ||
                result.getConfidenceScore().compareTo(BigDecimal.ZERO) < 0 ||
                result.getConfidenceScore().compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("confidenceScore must be between 0 and 1");
            }

            // Check forbidden words
            String allText = (result.getAiSummary() + " " + result.getAiExplanation() + " " +
                              String.join(" ", result.getAiRecommendation()) + " " +
                              result.getAiCampaignSuggestion().toString()).toLowerCase();

            if (allText.contains("platform") || allText.contains("marketplace") ||
                allText.contains("hoa hồng nền tảng") || allText.contains("đối tác cửa hàng") ||
                allText.contains("nhà cung cấp độc lập") || allText.contains("doanh thu toàn sàn")) {
                throw new IllegalArgumentException("Response contains forbidden business terminology");
            }

            return result;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            log.error("Failed to parse and validate AI response", e);
            throw new IllegalArgumentException("Invalid AI response format: " + e.getMessage(), e);
        }
    }

    public AIChatResult parseAndValidateChat(String rawGeminiResponse) {
        try {
            String jsonContent = extractJsonContent(rawGeminiResponse);
            AIChatResult result = objectMapper.readValue(jsonContent.trim(), AIChatResult.class);

            if (!StringUtils.hasText(result.getAnswer())) {
                throw new IllegalArgumentException("answer must not be empty");
            }
            if (result.getSuggestedActions() == null) {
                result.setSuggestedActions(new ArrayList<>());
            }
            if (result.getReferencedInsightIds() == null) {
                result.setReferencedInsightIds(new ArrayList<>());
            }
            validateConfidenceScore(result.getConfidenceScore());

            return result;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            log.error("Failed to parse and validate AI chat response", e);
            throw new IllegalArgumentException("Invalid AI chat response format: " + e.getMessage(), e);
        }
    }

    private String extractJsonContent(String rawGeminiResponse) {
        if (!StringUtils.hasText(rawGeminiResponse)) {
            throw new IllegalArgumentException("AI response must not be empty");
        }

        String jsonContent = rawGeminiResponse.trim();
        // Clean up possible markdown code blocks around json.
        if (jsonContent.startsWith("```json")) {
            jsonContent = jsonContent.substring(7);
        } else if (jsonContent.startsWith("```")) {
            jsonContent = jsonContent.substring(3);
        }
        jsonContent = jsonContent.trim();
        if (jsonContent.endsWith("```")) {
            jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
        }
        return jsonContent.trim();
    }

    private void validateConfidenceScore(BigDecimal confidenceScore) {
        if (confidenceScore == null ||
            confidenceScore.compareTo(BigDecimal.ZERO) < 0 ||
            confidenceScore.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confidenceScore must be between 0 and 1");
        }
    }
}
