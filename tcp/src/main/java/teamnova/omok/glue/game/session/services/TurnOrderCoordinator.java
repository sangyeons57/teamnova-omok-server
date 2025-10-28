package teamnova.omok.glue.game.session.services;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.TurnOrderRule;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Centralises turn order adjustments triggered by rules so the session pipeline
 * remains agnostic of individual rule behaviour.
 */
public final class TurnOrderCoordinator {
    public boolean apply(TurnOrderRule rule,
                         GameSessionRuleAccess access,
                         RuleRuntimeContext runtime) {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(runtime, "runtime");
        return rule.adjustTurnOrder(access, runtime);
    }
}
