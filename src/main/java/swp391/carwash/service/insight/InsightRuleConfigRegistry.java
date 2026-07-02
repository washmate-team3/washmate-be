package swp391.carwash.service.insight;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import swp391.carwash.entity.InsightRuleConfig;
import swp391.carwash.enums.InsightSeverity;

public class InsightRuleConfigRegistry {
    private static final InsightRuleConfigRegistry EMPTY = new InsightRuleConfigRegistry(Map.of());

    private final Map<String, InsightRuleConfig> configsByCode;

    private InsightRuleConfigRegistry(Map<String, InsightRuleConfig> configsByCode) {
        this.configsByCode = configsByCode;
    }

    public static InsightRuleConfigRegistry empty() {
        return EMPTY;
    }

    public static InsightRuleConfigRegistry from(Collection<InsightRuleConfig> configs) {
        return new InsightRuleConfigRegistry(configs.stream()
                .collect(Collectors.toMap(InsightRuleConfig::getRuleCode, Function.identity(), (left, right) -> left)));
    }

    public boolean active(String ruleCode) {
        InsightRuleConfig config = configsByCode.get(ruleCode);
        return config == null || Boolean.TRUE.equals(config.getActive());
    }

    public double threshold(String ruleCode, double fallback) {
        InsightRuleConfig config = configsByCode.get(ruleCode);
        BigDecimal threshold = config == null ? null : config.getThresholdValue();
        return threshold == null ? fallback : threshold.doubleValue();
    }

    public InsightSeverity severity(String ruleCode, InsightSeverity fallback) {
        InsightRuleConfig config = configsByCode.get(ruleCode);
        return config == null || config.getSeverity() == null ? fallback : config.getSeverity();
    }
}
