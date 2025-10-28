package teamnova.omok.glue.rule.runtime;

import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds configuration for how game rules should be selected. If a fixed rule list is
 * provided via environment or system properties it overrides the random selection.
 */
public final class RuleSelectionConfig {
    private static volatile List<RuleId> FIXED_RULE_OVERRIDE = List.of(RuleId.SEQUENTIAL_CONVERSION, RuleId.RANDOM_PLACEMENT);

    private final List<RuleId> fixedRuleIds;

    private RuleSelectionConfig(List<RuleId> fixedRuleIds) {
        this.fixedRuleIds = List.copyOf(Objects.requireNonNull(fixedRuleIds, "fixedRuleIds"));
    }

    public static RuleSelectionConfig load() {
        return new RuleSelectionConfig(FIXED_RULE_OVERRIDE);
    }

    public boolean hasFixedRuleOverride() {
        return !fixedRuleIds.isEmpty();
    }

    public List<RuleId> fixedRuleIds() {
        return Collections.unmodifiableList(fixedRuleIds);
    }

    public List<Rule> resolveFixedRules(RuleRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        if (fixedRuleIds.isEmpty()) {
            return List.of();
        }
        List<Rule> resolved = new ArrayList<>();
        for (RuleId id : fixedRuleIds) {
            Rule rule = registry.get(id);
            if (rule == null) {
                System.out.println("[RULE_LOG] Fixed rule " + id + " is not registered; skipping");
                continue;
            }
            resolved.add(rule);
        }
        return resolved;
    }

    public static void configureFixedRules(List<RuleId> ids) {
        if (ids == null || ids.isEmpty()) {
            FIXED_RULE_OVERRIDE = List.of();
            return;
        }
        List<RuleId> sanitized = new ArrayList<>();
        for (RuleId id : ids) {
            if (id != null) {
                sanitized.add(id);
            }
        }
        FIXED_RULE_OVERRIDE = sanitized.isEmpty() ? List.of() : List.copyOf(sanitized);
    }

    public static void configureFixedRules(RuleId... ids) {
        if (ids == null || ids.length == 0) {
            configureFixedRules(List.of());
            return;
        }
        List<RuleId> list = new ArrayList<>();
        for (RuleId id : ids) {
            if (id != null) {
                list.add(id);
            }
        }
        configureFixedRules(list);
    }

    public static void clearFixedRules() {
        FIXED_RULE_OVERRIDE = List.of();
    }
}
