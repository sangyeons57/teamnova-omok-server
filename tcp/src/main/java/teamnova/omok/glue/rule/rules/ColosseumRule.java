package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;

/**
 * 콜로세움: 가장자리 3칸을 방해돌로 막아 중앙 부분만 사용 가능하게 한다.
 * 호출 시점: 게임 시작 시.
 */
public final class ColosseumRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.COLOSSEUM,
        600
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Not implemented: sealing the edges with blockers should happen before the first move,
        // but doing so safely requires a dedicated board reset hook so other rules (e.g. mirrored
        // placement) know the blockers are system-generated. Pending that hook, this rule is idle.
    }
}
