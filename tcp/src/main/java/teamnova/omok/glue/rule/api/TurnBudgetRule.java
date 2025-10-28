package teamnova.omok.glue.rule.api;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Capability for rules that manage aggregate time budgets across turns for a
 * player.
 */
public interface TurnBudgetRule {
    boolean updateTurnBudget(GameSessionRuleAccess access, RuleRuntimeContext runtime);
}
