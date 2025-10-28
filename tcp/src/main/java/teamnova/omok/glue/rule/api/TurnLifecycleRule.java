package teamnova.omok.glue.rule.api;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Capability for rules that need a notification whenever the active turn
 * advances.
 */
public interface TurnLifecycleRule {
    void onTurnTick(GameSessionRuleAccess access, RuleRuntimeContext runtime);
}
