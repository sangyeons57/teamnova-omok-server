package teamnova.omok.glue.rule.api;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Optional capability for rules that need to mutate a move before it is
 * committed to the board. Implementations are invoked during the
 * {@link teamnova.omok.glue.rule.api.RuleTriggerKind#PRE_PLACEMENT} stage.
 */
public interface MoveMutationRule {
    void applyMoveMutation(GameSessionRuleAccess access, RuleRuntimeContext runtime);
}
