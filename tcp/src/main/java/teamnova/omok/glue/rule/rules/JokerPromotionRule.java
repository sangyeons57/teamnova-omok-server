package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;

/**
 * 조커: 모든 방해돌을 조커 돌로 변환한다.
 * 호출 시점: 게임 진행 중.
 */
public final class JokerPromotionRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.JOKER_PROMOTION,
        2_000
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Not implemented: automatically promoting all blockers to jokers alters balance
        // with no clear trigger cadence (continuous vs. one-shot). The board service
        // would also need coordinated metadata updates. This behaviour is deferred.
    }
}
