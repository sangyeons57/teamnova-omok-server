package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;

/**
 * 보자기: 보자기 NPC가 가장 가까운 돌을 추적하여 닿으면 방해돌로 변환한다.
 * 호출 시점: 전체 턴 종료 시.
 */
public final class HuntingClothRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.HUNTING_CLOTH,
        2_400
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Not implemented: simulating the "보자기" NPC requires persistent pathfinding
        // state, tick-driven updates, and broadcast mechanics beyond the current rule
        // hook surface. The engine lacks NPC ticking support, so this rule is disabled.
    }
}
