package teamnova.omok.rule;

import java.util.HashMap;
import java.util.Map;

public class RuleRegistry {
    public static RuleRegistry INSTANCE = null;

    private Map<RuleId,Rule> registry;

    public static RuleRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RuleRegistry();
        }
        return INSTANCE;
    }
    private RuleRegistry() {
        registry = new HashMap<>();
    }

    public void register(Rule rule) {
        registry.put(rule.getMetadata().id, rule);
    }

    public Rule get(RuleId id) {
        return registry.get(id);
    }
}
