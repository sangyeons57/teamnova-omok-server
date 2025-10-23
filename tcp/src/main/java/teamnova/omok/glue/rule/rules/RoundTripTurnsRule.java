package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 왕복: 플레이어 턴 순서가 왕복(정방향→역방향)으로 진행되도록 한다.
 * 호출 시점: 게임 진행 중.
 */
public final class RoundTripTurnsRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.ROUND_TRIP_TURNS,
        100
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        // Not implemented: supporting A→B→…→B→A sequencing requires a custom turn-advance strategy
        // and scheduler coordination. The current TurnService only supports simple iteration, so the
        // round-trip rule is not active yet.
    }
}
