package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swp391.carwash.dto.insight.InsightRuleConfigUpdateRequest;
import swp391.carwash.entity.InsightRuleConfig;
import swp391.carwash.enums.ComparisonOperator;
import swp391.carwash.enums.InsightSeverity;
import swp391.carwash.enums.InsightType;
import swp391.carwash.repository.InsightRuleConfigRepository;

@ExtendWith(MockitoExtension.class)
class InsightRuleConfigServiceTest {
    @Mock
    private InsightRuleConfigRepository insightRuleConfigRepository;

    private InsightRuleConfigService insightRuleConfigService;

    @BeforeEach
    void setUp() {
        insightRuleConfigService = new InsightRuleConfigService(insightRuleConfigRepository);
    }

    @Test
    void updateRuleConfigPersistsThresholdAndActiveFlag() {
        InsightRuleConfig config = InsightRuleConfig.builder()
                .id(1)
                .ruleCode("REVENUE_DROP")
                .ruleName("Revenue drop")
                .type(InsightType.REVENUE)
                .thresholdValue(new BigDecimal("15"))
                .comparisonOperator(ComparisonOperator.LESS_THAN)
                .severity(InsightSeverity.WARNING)
                .active(true)
                .build();
        when(insightRuleConfigRepository.findById(1)).thenReturn(Optional.of(config));
        when(insightRuleConfigRepository.save(any(InsightRuleConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = insightRuleConfigService.updateRuleConfig(
                1,
                new InsightRuleConfigUpdateRequest(new BigDecimal("20"), null, null, false, null));

        assertEquals(new BigDecimal("20"), response.thresholdValue());
        assertFalse(response.active());
    }
}
