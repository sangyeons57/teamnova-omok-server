package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 럭키 7: 7의 배수 턴에 승리 조건을 달성한 플레이어를 패배 처리한다.
 * 호출 시점: 게임 종료 시.
 */
public final class LuckySevenRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.LUCKY_SEVEN,
        2_100
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Not implemented: enforcing the defeat-on-seven condition requires OutcomeEvaluatingState
        // to react to rule metadata before declaring a winner. The evaluator currently finalises
        // turns immediately, so this rule is a placeholder until that flow is refactored.
    }
}
