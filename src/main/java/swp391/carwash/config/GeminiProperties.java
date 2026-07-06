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
