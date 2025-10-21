package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;

/**
 * 진화: 돌 생성 후 10턴이 지나면 자동으로 조커 돌로 변환한다.
 * 호출 시점: 게임 진행 중.
 */
public final class EvolutionRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.EVOLUTION,
        500
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Not implemented: ageing stones demands persistent turn counters per cell and synchronised
        // updates when stones are removed or moved by other rules. The existing stores do not track
        // per-stone lifetimes, so this behaviour cannot be realised yet.
    }
}
