package teamnova.omok.glue.game.session.services;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import java.util.Objects;

import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.runtime.GameSessionRuleBindings;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

public class RuleService {
    private static RuleService INSTANCE;
    public static RuleService Init() {
        INSTANCE = new RuleService();
        return INSTANCE;
    }
    public static RuleService getInstance() {
        return INSTANCE;
    }
    private RuleService() {
    }

    public void activateRules(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        Objects.requireNonNull(runtime, "runtime");
        GameSessionRuleBindings bindings = access.getRuleBindings();
        if (bindings == null) {
            return;
        }
        for (Rule rule : bindings.orderedRules()) {
            rule.invoke(access, runtime);
        }
    }
}
