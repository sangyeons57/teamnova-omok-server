package teamnova.omok.glue.rule.api;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Optional capability for rules that adjust the turn timing configuration for a
 * session. Implementations typically act during game start to customize the
 * base turn duration.
 */
public interface TurnTimingRule {
    boolean adjustTurnTiming(GameSessionRuleAccess access, RuleRuntimeContext runtime);
}
