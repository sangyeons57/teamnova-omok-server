package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 10수: 같은 종류의 돌이 10개 연속이면 해당 플레이어를 패배 처리한다.(연결되어있으면)
 * 호출 시점: 턴 종료 시.
 */
public final class TenChainEliminationRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.TEN_CHAIN_ELIMINATION,
        800
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        // Not implemented: detecting ten-stone chains and updating outcomes mid-turn would require
        // reworking the victory evaluation pipeline to support multiple simultaneous losers.
        // The current engine resolves wins only via OutcomeEvaluatingState, so this rule is inert.
    }
}
