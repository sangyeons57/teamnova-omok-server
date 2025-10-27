package teamnova.omok.glue.rule.api;

import java.util.Optional;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Optional capability for rules that wish to influence the game outcome.
 * Implementations can return an {@link OutcomeResolution} when the rule decides
 * to update player results or force finalization.
 */
public interface OutcomeRule {
    Optional<OutcomeResolution> resolveOutcome(GameSessionRuleAccess access, RuleRuntimeContext runtime);
}
