package teamnova.omok.glue.game.session.services;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.runtime.RuleRegistry;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

import java.util.Objects;

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
        for (RuleId id : access.getRuleIds()) {
            Rule rule = RuleRegistry.getInstance().get(id);
            if (rule != null) {
                rule.invoke(access, runtime);
            }
        }
    }
}
