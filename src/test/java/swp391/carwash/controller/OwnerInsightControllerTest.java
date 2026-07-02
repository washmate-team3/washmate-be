package swp391.carwash.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import swp391.carwash.dto.insight.AutoWashInsightsResponse;
import swp391.carwash.dto.insight.InsightPeriod;
import swp391.carwash.dto.insight.InsightSummary;
import swp391.carwash.enums.InsightAnalysisStatus;
import swp391.carwash.enums.InsightType;
import swp391.carwash.service.AIInsightChatService;
import swp391.carwash.service.AIInsightService;
import swp391.carwash.service.InsightRuleConfigService;
import swp391.carwash.service.InsightService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnerInsightControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private InsightService insightService;

    @MockitoBean
    private InsightRuleConfigService insightRuleConfigService;

    @MockitoBean
    private AIInsightService aiInsightService;

    @MockitoBean
    private AIInsightChatService aiInsightChatService;

    @Test
    void getInsightsReturnsOwnerInsightResponse() throws Exception {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        when(insightService.getInsights(eq(from), eq(to), eq(InsightType.ALL), isNull()))
                .thenReturn(new AutoWashInsightsResponse(
                        new InsightPeriod(from, to),
                        new InsightSummary(
                                new BigDecimal("85000000"),
                                1248,
                                882,
                                88,
                                180,
                                420,
                                120000,
                                35000,
                                new BigDecimal("70.67"),
                                new BigDecimal("7.05"),
                                "Rửa xe cơ bản",
                                "Rửa xe cao cấp",
                                "08:00 - 09:00",
                                "14:00 - 15:00"),
                        List.of(),
                        InsightAnalysisStatus.READY,
                        "Đã tạo insight từ dữ liệu vận hành hiện có."));

        mockMvc.perform(get("/api/owner/insights")
                        .param("fromDate", "2026-07-01")
                        .param("toDate", "2026-07-31")
                        .param("type", "ALL")
                        .param("status", "ALL")
                        .with(user("owner").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period.from").value("2026-07-01"))
                .andExpect(jsonPath("$.summary.totalRevenue").value(85000000))
                .andExpect(jsonPath("$.analysisStatus").value("READY"));
    }
}
