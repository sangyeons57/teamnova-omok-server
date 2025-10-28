package teamnova.omok.glue.rule.api;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Capability for rules that need to prepare the initial board layout before the
 * first turn begins.
 */
public interface BoardSetupRule {
    void setupBoard(GameSessionRuleAccess access, RuleRuntimeContext runtime);
}
