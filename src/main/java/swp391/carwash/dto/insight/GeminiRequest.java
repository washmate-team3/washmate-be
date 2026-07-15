package swp391.carwash.dto.insight;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeminiRequest {
    private List<Content> contents;
    private GenerationConfig generationConfig;

    @Data
    @Builder
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @Builder
    public static class Part {
        private String text;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GenerationConfig {
        private String responseMimeType;
        // temperature = 0 → gần như tất định, hết chuyện "mỗi lần ra khác".
        private Double temperature;
        // Giới hạn độ dài đầu ra để tránh sinh lê thê và chậm.
        private Integer maxOutputTokens;
        // Tắt "thinking" của gemini-2.5-flash (thinkingBudget = 0) để phản hồi nhanh.
        private ThinkingConfig thinkingConfig;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ThinkingConfig {
        private Integer thinkingBudget;
    }
}
