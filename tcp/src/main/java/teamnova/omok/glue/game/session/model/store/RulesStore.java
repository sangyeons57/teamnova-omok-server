package teamnova.omok.glue.game.session.model.store;

import teamnova.omok.glue.rule.api.RuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds mutable rule-selection state for a session.
 */
public final class RulesStore {
    private final List<RuleId> ruleIds = new ArrayList<>();
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    public RulesStore() {
    }

    public void setRuleIds(List<RuleId> ruleIds) {
        this.ruleIds.clear();
        this.ruleIds.addAll(ruleIds);
    }

    public List<RuleId> getRuleIds() {
        return ruleIds;
    }

    public Object getRuleData(String key) {
        return data.get(key);
    }

    public void putRuleData(String key, Object value) {
        data.put(key, value);
    }
    public void clearRuleData() {
        data.clear();
    }

    public boolean isRuleEmpty() {
        return ruleIds.isEmpty();
    }

}
