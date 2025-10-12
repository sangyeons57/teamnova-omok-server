package teamnova.omok.rule;

import teamnova.omok.state.game.manage.GameSessionStateType;

import java.util.List;
import java.util.Map;

public class RulesContext {
    public List<RuleId> activeRules;
    public Map<RuleType, RuleId[]> ruleMap;
    public RuleType currentType;
    public Map<String, Object> data;

    private RulesContext(List<RuleId> activeRules, Map<RuleType, RuleId[]> ruleMap) {
        this.activeRules = activeRules;
        this.ruleMap = ruleMap;
    }

    //rule은 결국 State에 따라서 동작할수밖에 없음
    public boolean setCurrentRuleByGameState(GameSessionStateType type) {
        RuleType ruleType = switch (type) {
            default -> RuleType.UNKNOWN;
        };

        this.currentType = ruleType;
        return ruleType != RuleType.UNKNOWN;
    }

    public RuleId[] getCurrentRuleIds() {
        return ruleMap.get(currentType);
    }

    public void activateRule(RuleId id) {
        Rule rule = RuleRegistry.getInstance().get(id);
        rule.invoke(this);
    }

    public void activateCurrentRule() {
        for(RuleId id : getCurrentRuleIds()) {
            activateRule(id);
        }
    }
}
