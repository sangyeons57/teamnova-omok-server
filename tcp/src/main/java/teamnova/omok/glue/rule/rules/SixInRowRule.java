package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 6목: 승리 조건을 5목에서 6목으로 확장한다. (5목 6목 둘다 허용)
 * 호출 시점: 게임 시작 시.
 */
public final class SixInRowRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.SIX_IN_ROW,
        400
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        // Not implemented: adjusting the victory condition requires refactoring
        // OutcomeEvaluatingState and BoardService (currently hard-coded to five-in-a-row).
        // Leaving this rule inert until the scoring pipeline is extended to accept
        // variable sequence lengths.
    }
}
