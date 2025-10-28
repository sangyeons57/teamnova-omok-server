package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.api.TurnOrderRule;
import teamnova.omok.glue.rule.runtime.RuleDataKeys;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 왕복: 플레이어 턴 순서가 왕복(정방향→역방향)으로 진행되도록 한다.
 * 호출 시점: 게임 진행 중.
 */
public final class RoundTripTurnsRule implements Rule, TurnOrderRule {
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
        // Primary behaviour executed via adjustTurnOrder.
    }

    @Override
    public boolean adjustTurnOrder(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null
            || runtime.triggerKind() != RuleTriggerKind.TURN_ROUND_COMPLETED
            || runtime.turnSnapshot() == null
            || runtime.turnSnapshot().order() == null) {
            return false;
        }
        int currentRound = runtime.turnSnapshot().counters().roundNumber();
        Integer lastRound = (Integer) access.getRuleData(RuleDataKeys.ROUND_TRIP_LAST_ROUND);
        if (lastRound != null && lastRound == currentRound) {
            return false;
        }
        List<String> currentOrder = runtime.turnSnapshot().order().userIds();
        if (currentOrder.size() <= 1) {
            return false;
        }
        Boolean directionFlag = (Boolean) access.getRuleData(RuleDataKeys.ROUND_TRIP_DIRECTION);
        boolean currentForward = directionFlag == null || directionFlag;
        boolean nextForward = !currentForward;
        List<String> nextOrder = new ArrayList<>(currentOrder);
        if (!nextForward) {
            Collections.reverse(nextOrder);
        }
        runtime.services().turnService().reseedOrder(
            runtime.stateContext().turns(),
            nextOrder,
            runtime.turnSnapshot().currentPlayerId(),
            System.currentTimeMillis()
        );
        access.putRuleData(RuleDataKeys.ROUND_TRIP_DIRECTION, nextForward);
        access.putRuleData(RuleDataKeys.ROUND_TRIP_LAST_ROUND, currentRound);
        return true;
    }
}
