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
 * 호출 시점: 라운드 종료 시 다음 라운드를 위해 순서를 재설정.
 */
public final class RoundTripTurnsRule implements Rule, TurnOrderRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.ROUND_TRIP_TURNS,
            2000
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
            || runtime.turnSnapshot() == null) {
            return false;
        }
        int currentRound = runtime.turnSnapshot().counters().roundNumber();
        Integer lastRound = (Integer) access.getRuleData(RuleDataKeys.ROUND_TRIP_LAST_ROUND);
        if (lastRound != null && lastRound == currentRound) {
            return false; // already applied for this round
        }
        if (runtime.stateContext() == null || runtime.stateContext().turns() == null) {
            return false;
        }
        var turnAccess = runtime.stateContext().turns();
        var orderSnapshot = turnAccess.order();
        if (orderSnapshot == null) {
            return false;
        }
        List<String> currentOrder = orderSnapshot.userIds();
        if (currentOrder.size() <= 1) {
            return false;
        }
        boolean forward = (currentRound % 2 == 1); // odd=forward, even=reverse
        List<String> nextOrder = new ArrayList<>(currentOrder);
        if (!forward) {
            Collections.reverse(nextOrder);
        }
        // First player of the new round should be the first of nextOrder
        String nextCurrent = nextOrder.get(0);
        runtime.services().turnService().reseedOrder(
            runtime.stateContext().turns(),
            nextOrder,
            nextCurrent,
            System.currentTimeMillis()
        );
        access.putRuleData(RuleDataKeys.ROUND_TRIP_LAST_ROUND, currentRound);
        return true;
    }
}
