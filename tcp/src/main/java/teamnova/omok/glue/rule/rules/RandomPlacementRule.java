package teamnova.omok.glue.rule.rules;

import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;

/**
 * 랜덤: 각 플레이어 돌이 50% 확률로 방해돌 또는 조커돌로 변형되도록 처리한다.
 * 호출 시점: 돌이 배치되기 전에.
 */
public final class RandomPlacementRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.RANDOM_PLACEMENT,
        300
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null || runtime.triggerKind() != RuleTriggerKind.PRE_PLACEMENT) {
            return;
        }
        GameSessionStateContext stateContext = runtime.stateContext();
        if (stateContext == null) {
            return;
        }
        TurnPersonalFrame frame = runtime.contextService().turn().currentPersonalTurn(stateContext);
        if (frame == null || !frame.hasActiveMove()) {
            return;
        }
        Stone original = frame.stone();
        if (original == null || !original.isPlayerStone()) {
            return;
        }
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll >= 0.5d) {
            return; // 50% chance to keep the original stone
        }
        Stone newStone = (roll < 0.25d) ? Stone.BLOCKER : Stone.JOKER;
        frame.stone(newStone);
    }
}
