package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.rule.api.RuleId;

import java.util.List;

public interface GameSessionRuleAccess extends GameSessionAccessInterface {
    void setRuleIds(List<RuleId> ruleIds);
    List<RuleId> getRuleIds();
    Object getRuleData(String key);
    void putRuleData(String key, Object value);
    void clearRuleData();
    boolean isRuleEmpty();
}
