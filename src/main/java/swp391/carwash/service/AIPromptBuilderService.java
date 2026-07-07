package swp391.carwash.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.carwash.dto.insight.InsightContext;
import swp391.carwash.entity.BusinessInsight;
import swp391.carwash.service.insight.MetricSnapshot;

@Service
@RequiredArgsConstructor
public class AIPromptBuilderService {
    private final ObjectMapper objectMapper;

    private static final String PROMPT_TEMPLATE = """
            Ban la tro ly phan tich du lieu kinh doanh cho he thong AutoWash.

            Boi canh:
            AutoWash la he thong quan ly rua xe noi bo cho mot doanh nghiep/tiem rua xe.
            Day khong phai platform nhieu doi tac.
            Khong duoc de cap den marketplace, doi tac, hoa hong nen tang, nha cung cap doc lap hoac doanh thu toan san.

            Nhiem vu:
            Dua tren insight rule-based va Context Package do backend da tinh san, hay tao phan giai thich va de xuat hanh dong cho chu doanh nghiep.

            Yeu cau:
            * Chi dung so lieu co trong Context Package.
            * Khong bia them so lieu moi, ti le moi, ten dich vu moi hoac khung gio moi.
            * Moi con so trong cau tra loi phai trace duoc ve headline, breakdown hoac trend.
            * Neu context khong du du lieu de ket luan, noi ro "chua du du lieu" thay vi suy doan.
            * Khong de cap du lieu ca nhan khach hang.
            * Viet bang tieng Viet, de hieu, thuc te.
            * De xuat hanh dong cu the, co the ap dung cho tiem rua xe.
            * Neu phu hop, de xuat mot chien dich khuyen mai hoac tich diem.
            * Khong nhac den AI/model trong noi dung tra ve.
            * Tra ve dung JSON format, khong them text ngoai JSON.

            Context Package:
            %s

            JSON response format:
            {
              "aiSummary": "...",
              "aiExplanation": "...",
              "aiRecommendation": ["...", "...", "..."],
              "aiCampaignSuggestion": {
                "campaignName": "...",
                "targetCustomers": "...",
                "offer": "...",
                "duration": "...",
                "goal": "..."
              },
              "confidenceScore": 0.0
            }
            """;

    private static final String DEEP_ANALYSIS_PROMPT_TEMPLATE = """
            Ban la tro ly phan tich du lieu kinh doanh cho AutoWash.

            Nhiem vu:
            Doc Metrics Snapshot da aggregate boi backend va tim pattern bat thuong ma rule co dinh co the bo sot.

            Guardrails:
            * Chi duoc dua insight dua tren metrics co trong snapshot.
            * Khong duoc dung raw PII, ten khach hang, email, phone, bien so xe.
            * evidence.metric bat buoc phai trung chinh xac mot key trong snapshot.metrics.
            * evidence.value bat buoc bang dung gia tri cua snapshot.metrics[evidence.metric].
            * Neu khong co pattern du manh, tra ve {"insights": []}.
            * Tra ve JSON thuan, khong them markdown hay text ngoai JSON.

            Metrics Snapshot:
            %s

            JSON response format:
            {
              "insights": [
                {
                  "type": "REVENUE_DROP",
                  "severity": "WARNING",
                  "claim": "Doanh thu giam manh trong ky nay",
                  "evidence": {
                    "metric": "revenue_change_percent",
                    "value": -23.00,
                    "period": "2026-07-01_to_2026-07-31"
                  },
                  "suggested_action": "Kiem tra dich vu va khung gio co doanh thu giam"
                }
              ]
            }
            """;

    public String buildInsightContext(BusinessInsight insight) {
        Map<String, Object> context = new HashMap<>();
        context.put("businessContext", "AutoWash is an internal car wash management system.");

        Map<String, Object> period = new HashMap<>();
        period.put("from", insight.getFromDate() != null ? insight.getFromDate().toString() : null);
        period.put("to", insight.getToDate() != null ? insight.getToDate().toString() : null);
        context.put("period", period);

        Map<String, Object> baseInsight = new HashMap<>();
        baseInsight.put("ruleCode", insight.getRuleCode());
        baseInsight.put("type", insight.getType() != null ? insight.getType().name() : null);
        baseInsight.put("severity", insight.getSeverity() != null ? insight.getSeverity().name() : null);
        baseInsight.put("title", insight.getTitle());
        baseInsight.put("summary", insight.getSummary());
        baseInsight.put("evidence", insight.getEvidence());
        baseInsight.put("meaning", insight.getMeaning());
        baseInsight.put("recommendation", insight.getRecommendation());
        baseInsight.put("relatedMetric", insight.getRelatedMetric());
        context.put("baseInsight", baseInsight);

        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error building insight context JSON", e);
        }
    }

    public String buildContextJson(InsightContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error building insight context JSON", e);
        }
    }

    public String buildSnapshotJson(MetricSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error building metric snapshot JSON", e);
        }
    }

    public String buildPrompt(String contextJson) {
        return String.format(PROMPT_TEMPLATE, contextJson);
    }

    public String buildDeepAnalysisPrompt(String snapshotJson) {
        return String.format(DEEP_ANALYSIS_PROMPT_TEMPLATE, snapshotJson);
    }
}
