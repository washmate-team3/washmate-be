package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swp391.carwash.config.GeminiProperties;
import swp391.carwash.dto.insight.AIChatRequest;
import swp391.carwash.dto.insight.AIChatResult;
import swp391.carwash.entity.BusinessInsight;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightStatus;
import swp391.carwash.enums.InsightType;
import swp391.carwash.repository.BusinessInsightRepository;
import swp391.carwash.service.insight.InsightAnalysisContext;
import swp391.carwash.service.insight.InsightMetrics;
import swp391.carwash.service.insight.InsightTestData;
import swp391.carwash.service.insight.rule.RevenueInsightRule;

@ExtendWith(MockitoExtension.class)
class AIInsightChatServiceTest {
    @Mock
    private BusinessInsightRepository businessInsightRepository;
    @Mock
    private ReportAggregationService reportAggregationService;
    @Mock
    private AIPromptBuilderService promptBuilderService;
    @Mock
    private GeminiClient geminiClient;
    @Mock
    private AIResponseValidatorService validatorService;

    private AIInsightChatService aiInsightChatService;

    @BeforeEach
    void setUp() {
        aiInsightChatService = new AIInsightChatService(
                businessInsightRepository,
                reportAggregationService,
                promptBuilderService,
                geminiClient,
                validatorService,
                new GeminiProperties(),
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void chatUsesFocusedInsightPeriodWhenRequestHasInsightId() {
        BusinessInsight insight = validInsight();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        AIChatRequest request = new AIChatRequest(
                "Tai sao doanh thu tang?",
                null,
                1,
                null,
                null,
                InsightType.ALL,
                "ALL",
                List.of());
        when(businessInsightRepository.findById(1)).thenReturn(Optional.of(insight));
        when(reportAggregationService.aggregate(from, to)).thenReturn(businessContext(from, to));
        when(businessInsightRepository.findForOwnerInsights(from, to, null, null)).thenReturn(List.of(insight));
        when(promptBuilderService.buildChatPrompt(anyString(), eq("Tai sao doanh thu tang?"), eq(List.of())))
                .thenReturn("prompt");
        when(geminiClient.generateContent("prompt")).thenReturn("{}");
        when(validatorService.parseAndValidateChat("{}")).thenReturn(AIChatResult.builder()
                .answer("Doanh thu tang do so don hoan tat tot hon.")
                .suggestedActions(List.of("Theo doi dich vu dong gop chinh"))
                .referencedInsightIds(List.of(1))
                .confidenceScore(new BigDecimal("0.77"))
                .build());

        var response = aiInsightChatService.chat(request);

        assertEquals("Doanh thu tang do so don hoan tat tot hon.", response.getAnswer());
        assertEquals(1, response.getReferencedInsightIds().getFirst());
    }

    private InsightAnalysisContext businessContext(LocalDate from, LocalDate to) {
        var booking = InsightTestData.booking(1, 1, BookingStatus.COMPLETED, from);
        InsightMetrics current = InsightTestData.metrics(
                from,
                to,
                List.of(booking),
                List.of(InsightTestData.paidInvoice(1, booking, "120000", OffsetDateTime.parse("2026-07-01T10:00:00+07:00"))),
                List.of(),
                List.of(),
                List.of(booking.getService()),
                Set.of(),
                0);
        return new InsightAnalysisContext(current, current, OffsetDateTime.parse("2026-07-31T10:00:00+07:00"));
    }

    private BusinessInsight validInsight() {
        return BusinessInsight.builder()
                .id(1)
                .ruleCode(RevenueInsightRule.REVENUE_GROWTH)
                .type(InsightType.REVENUE)
                .severity(InsightSeverity.POSITIVE)
                .title("Revenue growth")
                .summary("Revenue increased.")
                .evidence("Current revenue is higher than previous period.")
                .meaning("Positive trend.")
                .recommendation("Keep monitoring services.")
                .fromDate(LocalDate.of(2026, 7, 1))
                .toDate(LocalDate.of(2026, 7, 31))
                .status(InsightStatus.NEW)
                .createdAt(OffsetDateTime.parse("2026-07-31T10:00:00+07:00"))
                .build();
    }
}
