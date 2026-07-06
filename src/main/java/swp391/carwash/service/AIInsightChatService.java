package swp391.carwash.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import swp391.carwash.config.GeminiProperties;
import swp391.carwash.dto.insight.AIChatRequest;
import swp391.carwash.dto.insight.AIChatResponse;
import swp391.carwash.dto.insight.AIChatResult;
import swp391.carwash.entity.BusinessInsight;
import swp391.carwash.enums.InsightStatus;
import swp391.carwash.enums.InsightType;
import swp391.carwash.repository.BusinessInsightRepository;
import swp391.carwash.service.insight.InsightAnalysisContext;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIInsightChatService {
    private static final int MAX_CONTEXT_INSIGHTS = 12;

    private final BusinessInsightRepository businessInsightRepository;
    private final ReportAggregationService reportAggregationService;
    private final AIPromptBuilderService promptBuilderService;
    private final GeminiClient geminiClient;
    private final AIResponseValidatorService validatorService;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    public AIChatResponse chat(AIChatRequest request) {
        BusinessInsight focusedInsight = request.insightId() == null
                ? null
                : businessInsightRepository.findById(request.insightId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Insight not found"));
        DateRange range = resolveRange(request.fromDate(), request.toDate(), focusedInsight);
        InsightAnalysisContext analysisContext = reportAggregationService.aggregate(range.from(), range.to());

        List<BusinessInsight> contextInsights = loadContextInsights(request, range, focusedInsight);
        String contextJson = buildChatContextJson(range, analysisContext, focusedInsight, contextInsights);
        String prompt = promptBuilderService.buildChatPrompt(contextJson, request.effectiveQuestion(), request.history());

        try {
            String rawResponse = geminiClient.generateContent(prompt);
            AIChatResult result = validatorService.parseAndValidateChat(rawResponse);
            return AIChatResponse.builder()
                    .answer(result.getAnswer())
                    .suggestedActions(result.getSuggestedActions())
                    .referencedInsightIds(result.getReferencedInsightIds())
                    .confidenceScore(result.getConfidenceScore())
                    .aiModel(geminiProperties.getModel())
                    .promptVersion(geminiProperties.getPrompt().getVersion())
                    .generatedAt(OffsetDateTime.now())
                    .build();
        } catch (IllegalArgumentException e) {
            log.error("Validation error for Gemini chat response: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.valueOf(422), "Gemini chat response invalid: " + e.getMessage());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating AI chat response", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error connecting to AI service");
        }
    }

    private List<BusinessInsight> loadContextInsights(
            AIChatRequest request,
            DateRange range,
            BusinessInsight focusedInsight) {
        InsightType typeFilter = normalizeType(request.type());
        InsightStatus statusFilter = parseStatusFilter(request.status());
        List<BusinessInsight> persistedInsights = businessInsightRepository
                .findForOwnerInsights(range.from(), range.to(), typeFilter, statusFilter)
                .stream()
                .sorted(insightComparator())
                .toList();

        ArrayList<BusinessInsight> contextInsights = new ArrayList<>();
        if (focusedInsight != null) {
            contextInsights.add(focusedInsight);
        }
        for (BusinessInsight insight : persistedInsights) {
            if (contextInsights.size() >= MAX_CONTEXT_INSIGHTS) {
                break;
            }
            if (focusedInsight == null || !Objects.equals(focusedInsight.getId(), insight.getId())) {
                contextInsights.add(insight);
            }
        }
        return contextInsights;
    }

    private String buildChatContextJson(
            DateRange range,
            InsightAnalysisContext analysisContext,
            BusinessInsight focusedInsight,
            List<BusinessInsight> contextInsights) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("businessContext", "AutoWash is an internal car wash management system for one business/car wash shop.");
        context.put("period", Map.of(
                "from", range.from().toString(),
                "to", range.to().toString()));
        context.put("summary", analysisContext.current().toSummary());
        context.put("analysisStatus", analysisContext.current().hasBusinessData() ? "READY" : "INSUFFICIENT_DATA");
        context.put("focusedInsightId", focusedInsight == null ? null : focusedInsight.getId());
        context.put("insights", contextInsights.stream()
                .map(this::toInsightContext)
                .toList());

        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error building AI chat context");
        }
    }

    private Map<String, Object> toInsightContext(BusinessInsight insight) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", insight.getId());
        item.put("ruleCode", insight.getRuleCode());
        item.put("type", insight.getType());
        item.put("severity", insight.getSeverity());
        item.put("status", insight.getStatus());
        item.put("title", insight.getTitle());
        item.put("summary", insight.getSummary());
        item.put("evidence", insight.getEvidence());
        item.put("meaning", insight.getMeaning());
        item.put("recommendation", insight.getRecommendation());
        item.put("relatedMetric", insight.getRelatedMetric());
        item.put("fromDate", insight.getFromDate());
        item.put("toDate", insight.getToDate());
        return item;
    }

    private DateRange resolveRange(LocalDate fromDate, LocalDate toDate, BusinessInsight focusedInsight) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedTo = toDate != null
                ? toDate
                : focusedInsight != null && focusedInsight.getToDate() != null ? focusedInsight.getToDate() : today;
        LocalDate resolvedFrom = fromDate != null
                ? fromDate
                : focusedInsight != null && focusedInsight.getFromDate() != null
                        ? focusedInsight.getFromDate()
                        : resolvedTo.withDayOfMonth(1);

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be before or equal to toDate");
        }
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private InsightStatus parseStatusFilter(String status) {
        if (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return InsightStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid insight status filter");
        }
    }

    private InsightType normalizeType(InsightType type) {
        return type == null || type == InsightType.ALL ? null : type;
    }

    private Comparator<BusinessInsight> insightComparator() {
        return Comparator
                .comparingInt((BusinessInsight insight) -> insight.getSeverity().getPriority())
                .thenComparing(BusinessInsight::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}
