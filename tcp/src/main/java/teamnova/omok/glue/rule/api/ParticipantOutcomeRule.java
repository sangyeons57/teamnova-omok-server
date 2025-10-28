package teamnova.omok.glue.rule.api;

import java.util.Map;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * Capability for rules that introduce additional participants or seed initial
 * outcome assignments before evaluation begins.
 */
public interface ParticipantOutcomeRule {
    Map<String, PlayerResult> registerParticipantOutcomes(GameSessionRuleAccess access,
                                                          RuleRuntimeContext runtime);
}
