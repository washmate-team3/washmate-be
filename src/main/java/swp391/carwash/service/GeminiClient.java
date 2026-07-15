package swp391.carwash.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import swp391.carwash.config.GeminiProperties;
import swp391.carwash.dto.insight.GeminiRequest;
import swp391.carwash.dto.insight.GeminiResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
@Slf4j
public class GeminiClient {
    private final RestTemplate restTemplate;
    private final GeminiProperties geminiProperties;

    public GeminiClient(GeminiProperties geminiProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
        this.geminiProperties = geminiProperties;
    }

    public String generateContent(String prompt) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini API key is not configured");
        }

        String url = UriComponentsBuilder.fromUriString(geminiProperties.getApi().getBaseUrl())
                .pathSegment(geminiProperties.getModel() + ":generateContent")
                .queryParam("key", geminiProperties.getApi().getKey())
                .toUriString();

        GeminiRequest requestBody = GeminiRequest.builder()
                .contents(List.of(
                        GeminiRequest.Content.builder()
                                .parts(List.of(
                                        GeminiRequest.Part.builder()
                                                .text(prompt)
                                                .build()
                                ))
                                .build()
                ))
                .generationConfig(buildGenerationConfig())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GeminiRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            GeminiResponse response = restTemplate.postForObject(url, requestEntity, GeminiResponse.class);
            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                GeminiResponse.Candidate candidate = response.getCandidates().get(0);
                if (candidate.getContent() != null && candidate.getContent().getParts() != null && !candidate.getContent().getParts().isEmpty()) {
                    return candidate.getContent().getParts().get(0).getText();
                }
            }
            throw new RuntimeException("Empty response from Gemini API");
        } catch (RestClientResponseException e) {
            log.error("Gemini API returned status {}", e.getStatusCode().value());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini API returned an error");
        } catch (ResourceAccessException e) {
            log.error("Gemini API request timed out or was unreachable");
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gemini API timeout or unreachable");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected Gemini API client error: {}", e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini API error");
        }
    }

    private GeminiRequest.GenerationConfig buildGenerationConfig() {
        GeminiRequest.GenerationConfig.GenerationConfigBuilder config = GeminiRequest.GenerationConfig.builder()
                .responseMimeType("application/json")
                .temperature(geminiProperties.getTemperature())
                .maxOutputTokens(geminiProperties.getMaxOutputTokens());
        if (geminiProperties.getThinkingBudget() != null) {
            config.thinkingConfig(GeminiRequest.ThinkingConfig.builder()
                    .thinkingBudget(geminiProperties.getThinkingBudget())
                    .build());
        }
        return config.build();
    }

    public boolean isConfigured() {
        String apiKey = geminiProperties.getApi().getKey();
        return StringUtils.hasText(apiKey) && !apiKey.startsWith("${");
    }
}
