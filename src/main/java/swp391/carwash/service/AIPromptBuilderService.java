package swp391.carwash.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.carwash.dto.insight.AIChatMessage;
import swp391.carwash.entity.BusinessInsight;

@Service
@RequiredArgsConstructor
public class AIPromptBuilderService {
    private final ObjectMapper objectMapper;

    private static final String PROMPT_TEMPLATE = """
            Bạn là trợ lý phân tích dữ liệu kinh doanh cho hệ thống AutoWash.

            Bối cảnh:
            AutoWash là hệ thống quản lý rửa xe nội bộ cho một doanh nghiệp/tiệm rửa xe.
            Đây không phải platform nhiều đối tác.
            Không được đề cập đến marketplace, đối tác, hoa hồng nền tảng, nhà cung cấp độc lập hoặc doanh thu toàn sàn.

            Nhiệm vụ:
            Dựa trên insight rule-based và dữ liệu được cung cấp, hãy tạo phần giải thích và đề xuất hành động cho chủ doanh nghiệp.

            Yêu cầu:
            * Chỉ dựa trên dữ liệu được cung cấp.
            * Không bịa thêm số liệu mới.
            * Không đưa ra kết luận nếu dữ liệu không hỗ trợ.
            * Không đề cập dữ liệu cá nhân khách hàng.
            * Viết bằng tiếng Việt, dễ hiểu, thực tế.
            * Đề xuất hành động cụ thể, có thể áp dụng cho tiệm rửa xe.
            * Nếu phù hợp, đề xuất một chiến dịch khuyến mãi hoặc tích điểm.
            * Không nhắc đến AI/model trong nội dung trả về.
            * Trả về đúng JSON format, không thêm text ngoài JSON.

            Dữ liệu:
            %s

            JSON response format:
            {
              "aiSummary": "...",
              "aiExplanation": "...",
              "aiRecommendation": ["...", "...", "..."],
              "aiCampaignSuggestion": {
                "campaignName": "...",
                "targetCustomers": "...",
                "offer": "...",
                "duration": "...",
                "goal": "..."
              },
              "confidenceScore": 0.0
            }
            """;

    private static final String CHAT_PROMPT_TEMPLATE = """
            You are the business analysis assistant for AutoWash.

            Business context:
            AutoWash is an internal car wash management system for one business/car wash shop.
            Do not mention marketplaces, platform commission, independent providers, or marketplace-wide revenue.

            Task:
            Answer the owner's question using only the supplied business summary and saved rule-based insights.
            If the supplied data is not enough, say that clearly and suggest what data should be checked next.
            Do not invent new numbers, customer identities, or facts not present in the supplied data.
            Write in Vietnamese, practical and concise.
            Do not mention AI/model in the answer content.
            Return valid JSON only, with no text outside JSON.

            Business data:
            %s

            Recent conversation:
            %s

            Owner question:
            %s

            JSON response format:
            {
              "answer": "...",
              "suggestedActions": ["...", "..."],
              "referencedInsightIds": [1, 2],
              "confidenceScore": 0.0
            }
            """;

    public String buildInsightContext(BusinessInsight insight) {
        Map<String, Object> context = new HashMap<>();
        context.put("businessContext", "AutoWash là hệ thống quản lý rửa xe nội bộ cho một doanh nghiệp/tiệm rửa xe.");

        Map<String, Object> period = new HashMap<>();
        period.put("from", insight.getFromDate() != null ? insight.getFromDate().toString() : null);
        period.put("to", insight.getToDate() != null ? insight.getToDate().toString() : null);
        context.put("period", period);

        Map<String, Object> baseInsight = new HashMap<>();
        baseInsight.put("ruleCode", insight.getRuleCode());
        baseInsight.put("type", insight.getType() != null ? insight.getType().name() : null);
        baseInsight.put("severity", insight.getSeverity() != null ? insight.getSeverity().name() : null);
        baseInsight.put("title", insight.getTitle());
        baseInsight.put("summary", insight.getSummary());
        baseInsight.put("evidence", insight.getEvidence());
        baseInsight.put("meaning", insight.getMeaning());
        baseInsight.put("recommendation", insight.getRecommendation());
        baseInsight.put("relatedMetric", insight.getRelatedMetric());
        context.put("baseInsight", baseInsight);

        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error building insight context JSON", e);
        }
    }

    public String buildPrompt(String contextJson) {
        return String.format(PROMPT_TEMPLATE, contextJson);
    }

    public String buildChatPrompt(String contextJson, String question, List<AIChatMessage> history) {
        try {
            String questionJson = objectMapper.writeValueAsString(question);
            String historyJson = objectMapper.writeValueAsString(history == null ? List.of() : history);
            return String.format(CHAT_PROMPT_TEMPLATE, contextJson, historyJson, questionJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error building AI chat prompt", e);
        }
    }
}
