package teamnova.omok.domain.rule;

import teamnova.omok.domain.rule.model.Rule;
import teamnova.omok.domain.rule.model.RuleId;
import teamnova.omok.domain.rule.model.RuleMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RuleRegistry {
    private static RuleRegistry INSTANCE;

    private final Map<RuleId, Rule> registry;

    public static synchronized RuleRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RuleRegistry();
        }
        return INSTANCE;
    }

    private RuleRegistry() {
        registry = new ConcurrentHashMap<>();
    }

    public void register(Rule rule) {
        Objects.requireNonNull(rule, "rule");
        RuleMetadata metadata = Objects.requireNonNull(rule.getMetadata(), "metadata");
        Objects.requireNonNull(metadata.id, "rule id");
        registry.put(metadata.id, rule);
    }

    public Rule get(RuleId id) {
        return id == null ? null : registry.get(id);
    }

    public List<Rule> eligibleRules(int lowestParticipantScore) {
        List<Rule> result = new ArrayList<>();
        for (Rule rule : registry.values()) {
            RuleMetadata metadata = rule.getMetadata();
            if (metadata == null) {
                continue;
            }
            if (metadata.limitScore <= lowestParticipantScore) {
                result.add(rule);
            }
        }
        return result;
    }
}
