package teamnova.omok.glue.game.session.interfaces.session;

import java.util.List;

import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.runtime.GameSessionRuleBindings;

public interface GameSessionRuleAccess extends GameSessionAccessInterface {
    void setRuleIds(List<RuleId> ruleIds);
    List<RuleId> getRuleIds();
    void setRuleBindings(GameSessionRuleBindings ruleBindings);
    GameSessionRuleBindings getRuleBindings();
    Object getRuleData(String key);
    void putRuleData(String key, Object value);
    void clearRuleData();
    boolean isRuleEmpty();
}
