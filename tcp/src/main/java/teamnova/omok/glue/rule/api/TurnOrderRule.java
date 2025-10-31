package teamnova.omok.glue.rule.api;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Optional capability for rules that need to reseed or otherwise adjust the
 * active turn order. Implementations are expected to call into the provided
 * services to perform safe reseeding.
 */
public interface TurnOrderRule {
    TurnOrderAdjustment adjustTurnOrder(GameSessionRuleAccess access,
                                        RuleRuntimeContext runtime,
                                        TurnOrderAdjustment current);
}
