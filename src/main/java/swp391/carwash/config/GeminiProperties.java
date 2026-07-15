package swp391.carwash.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    private Api api = new Api();
    private String model = "gemini-2.5-flash";
    private Prompt prompt = new Prompt();
    // temperature 0 → ổn định (cùng prompt ra cùng kết quả). Tăng nhẹ nếu muốn đa dạng hơn.
    private Double temperature = 0.0;
    // Giới hạn token đầu ra để không sinh quá dài (đủ cho enrichment/deep analysis).
    private Integer maxOutputTokens = 2048;
    // 0 = tắt thinking của gemini-2.5-flash → nhanh hơn. Đặt null nếu model không hỗ trợ.
    private Integer thinkingBudget = 0;

    @Getter
    @Setter
    public static class Api {
        private String key;
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models";
    }

    @Getter
    @Setter
    public static class Prompt {
        private String version = "v1";
    }
}
