package teamnova.omok.glue.rule.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;

/**
 * Resolves rule identifiers into live instances and builds session-specific
 * capability views to avoid repeated registry scans at runtime.
 */
public final class RuleEngine {
    private final RuleRegistry registry;

    public RuleEngine(RuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public GameSessionRuleBindings createBindings(List<RuleId> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return GameSessionRuleBindings.empty();
        }
        List<Rule> resolved = new ArrayList<>(ruleIds.size());
        for (RuleId id : ruleIds) {
            if (id == null) {
                continue;
            }
            Rule rule = registry.get(id);
            if (rule != null) {
                resolved.add(rule);
            }
        }
        if (resolved.isEmpty()) {
            return GameSessionRuleBindings.empty();
        }
        return GameSessionRuleBindings.of(resolved);
    }
}
