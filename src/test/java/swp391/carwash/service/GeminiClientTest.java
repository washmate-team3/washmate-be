package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import swp391.carwash.config.GeminiProperties;

class GeminiClientTest {
    @Test
    void generateContentReturnsServiceUnavailableWhenApiKeyIsMissing() {
        GeminiClient client = new GeminiClient(new GeminiProperties());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> client.generateContent("prompt"));

        assertFalse(client.isConfigured());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }
}
