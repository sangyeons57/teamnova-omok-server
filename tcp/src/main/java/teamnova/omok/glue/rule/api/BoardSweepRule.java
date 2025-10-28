package teamnova.omok.glue.rule.api;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Capability for rules that need to periodically scan and mutate the entire
 * board state after turns conclude.
 */
public interface BoardSweepRule {
    void sweepBoard(GameSessionRuleAccess access, RuleRuntimeContext runtime);
}
