package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.TurnOrderRule;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 순서뽑기: 매 턴마다 플레이어 순서를 무작위로 재배치한다.
 * 호출 시점: 전체 턴 종료 시.
 */
public final class TurnOrderShuffleRule implements Rule, TurnOrderRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.TURN_ORDER_SHUFFLE,
        0
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        // Not implemented: reordering players mid-session needs TurnService reseeding and message
        // broadcasts so clients know the updated order. That capability is not exposed through
        // the current state context, so this rule is left as a stub.
    }

    @Override
    public boolean adjustTurnOrder(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Not implemented: see invoke rationale above.
        return false;
    }
}
