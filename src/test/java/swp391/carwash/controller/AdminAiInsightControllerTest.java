package swp391.carwash.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import swp391.carwash.dto.CustomerAiInsightAdminResponse;
import swp391.carwash.service.AiInsightGenerationService;

@ExtendWith(MockitoExtension.class)
class AdminAiInsightControllerTest {
    @Mock
    private AiInsightGenerationService aiInsightGenerationService;

    private AdminAiInsightController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminAiInsightController(aiInsightGenerationService);
    }

    @Test
    void generateInsightsDelegatesToService() {
        List<CustomerAiInsightAdminResponse> expected = List.of();
        when(aiInsightGenerationService.generateInsights(1, "2026-06")).thenReturn(expected);

        List<CustomerAiInsightAdminResponse> response = controller.generateInsights(1, "2026-06");

        assertSame(expected, response);
        verify(aiInsightGenerationService).generateInsights(1, "2026-06");
    }

    @Test
    void getInsightsDelegatesToService() {
        List<CustomerAiInsightAdminResponse> expected = List.of();
        when(aiInsightGenerationService.getInsights(1, "2026-06")).thenReturn(expected);

        List<CustomerAiInsightAdminResponse> response = controller.getInsights(1, "2026-06");

        assertSame(expected, response);
        verify(aiInsightGenerationService).getInsights(1, "2026-06");
    }

    @Test
    void usesShortAdminInsightRoutes() throws NoSuchMethodException {
        PostMapping generateMapping = AdminAiInsightController.class
                .getMethod("generateInsights", Integer.class, String.class)
                .getAnnotation(PostMapping.class);
        GetMapping getMapping = AdminAiInsightController.class
                .getMethod("getInsights", Integer.class, String.class)
                .getAnnotation(GetMapping.class);

        assertTrue(List.of(generateMapping.value()).contains("/api/admin/insights/generate"));
        assertTrue(List.of(getMapping.value()).contains("/api/admin/insights"));
    }
}
