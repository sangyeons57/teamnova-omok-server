package teamnova.omok.glue.rule;


import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;

public interface Rule {
    RuleMetadata getMetadata();

    void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime);
}
