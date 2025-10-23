package teamnova.omok.glue.rule.api;


import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

public interface Rule {
    RuleMetadata getMetadata();

    void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime);
}
