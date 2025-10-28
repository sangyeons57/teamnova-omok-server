package teamnova.omok.glue.rule.api;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Capability for rules that temporarily conceal stone placements and reveal
 * them at a later lifecycle moment.
 */
public interface HiddenPlacementRule {
    boolean queueHiddenPlacement(GameSessionRuleAccess access, RuleRuntimeContext runtime);

    void revealHiddenPlacements(GameSessionRuleAccess access, RuleRuntimeContext runtime);
}
