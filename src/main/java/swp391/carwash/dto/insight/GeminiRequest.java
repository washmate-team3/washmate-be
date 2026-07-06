package swp391.carwash.dto.insight;

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
    public static class GenerationConfig {
        private String responseMimeType;
    }
}
