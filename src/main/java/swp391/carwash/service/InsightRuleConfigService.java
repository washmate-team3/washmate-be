package swp391.carwash.service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.insight.InsightRuleConfigResponse;
import swp391.carwash.dto.insight.InsightRuleConfigUpdateRequest;
import swp391.carwash.entity.InsightRuleConfig;
import swp391.carwash.repository.InsightRuleConfigRepository;

@Service
@RequiredArgsConstructor
public class InsightRuleConfigService {
    private final InsightRuleConfigRepository insightRuleConfigRepository;

    @Transactional(readOnly = true)
    public List<InsightRuleConfigResponse> getRuleConfigs() {
        return insightRuleConfigRepository.findAll().stream()
                .sorted(Comparator.comparing(InsightRuleConfig::getType).thenComparing(InsightRuleConfig::getRuleCode))
                .map(InsightRuleConfigResponse::from)
                .toList();
    }

    @Transactional
    public InsightRuleConfigResponse updateRuleConfig(Integer id, InsightRuleConfigUpdateRequest request) {
        InsightRuleConfig config = insightRuleConfigRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Insight rule config not found"));
        if (request.thresholdValue() != null) {
            config.setThresholdValue(request.thresholdValue());
        }
        if (request.comparisonOperator() != null) {
            config.setComparisonOperator(request.comparisonOperator());
        }
        if (request.severity() != null) {
            config.setSeverity(request.severity());
        }
        if (request.active() != null) {
            config.setActive(request.active());
        }
        if (request.description() != null) {
            config.setDescription(request.description());
        }
        config.setUpdatedAt(OffsetDateTime.now());
        return InsightRuleConfigResponse.from(insightRuleConfigRepository.save(config));
    }
}
