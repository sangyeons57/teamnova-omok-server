package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleRuntimeContext;

import java.util.List;
import java.util.function.Function;

public interface GameSessionRuleAccess extends GameSessionAccessInterface {
    void setRuleIds(List<RuleId> ruleIds);
    List<RuleId> getRuleIds();
    Object getRuleData(String key);
    void putRuleData(String key, Object value);
    void clearRuleData();
    boolean isRuleEmpty();
}
