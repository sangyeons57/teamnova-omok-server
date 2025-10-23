package teamnova.omok.glue.game.session.model.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.runtime.GameSessionRuleBindings;

/**
 * Holds mutable rule-selection state for a session.
 */
public final class RulesStore {
    private final List<RuleId> ruleIds = new ArrayList<>();
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private volatile GameSessionRuleBindings ruleBindings = GameSessionRuleBindings.empty();

    public RulesStore() {
    }

    public void setRuleIds(List<RuleId> ruleIds) {
        this.ruleIds.clear();
        this.ruleIds.addAll(ruleIds);
    }

    public List<RuleId> getRuleIds() {
        return ruleIds;
    }

    public void setRuleBindings(GameSessionRuleBindings ruleBindings) {
        this.ruleBindings = Objects.requireNonNull(ruleBindings, "ruleBindings");
    }

    public GameSessionRuleBindings getRuleBindings() {
        return ruleBindings;
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
