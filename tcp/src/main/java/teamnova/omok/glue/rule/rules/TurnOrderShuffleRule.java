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
 * 순서뽑기: 매 라운드가 끝날 때 다음 라운드를 위해 플레이어 순서를 무작위로 재배치한다.
 */
public final class TurnOrderShuffleRule implements Rule, TurnOrderRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.TURN_ORDER_SHUFFLE,
        2000
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        // Primary behaviour is implemented via adjustTurnOrder at TURN_ROUND_COMPLETED.
    }

    @Override
    public boolean adjustTurnOrder(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null
            || runtime.triggerKind() != RuleTriggerKind.TURN_ROUND_COMPLETED
            || runtime.turnSnapshot() == null) {
            return false;
        }
        int currentRound = runtime.turnSnapshot().counters().roundNumber();
        Integer lastRound = (Integer) access.getRuleData(RuleDataKeys.SHUFFLE_LAST_ROUND);
        if (lastRound != null && lastRound == currentRound) {
            return false; // already applied this round
        }
        if (runtime.stateContext() == null || runtime.stateContext().turns() == null) {
            return false;
        }
        var turnAccess = runtime.stateContext().turns();
        var orderSnapshot = turnAccess.order();
        if (orderSnapshot == null) {
            return false;
        }
        List<String> order = orderSnapshot.userIds();
        if (order.size() <= 1) {
            return false;
        }
        List<String> next = new ArrayList<>(order);
        Collections.shuffle(next);
        String nextCurrent = next.get(0);
        runtime.services().turnService().reseedOrder(
            runtime.stateContext().turns(),
            next,
            nextCurrent,
            System.currentTimeMillis()
        );
        access.putRuleData(RuleDataKeys.SHUFFLE_LAST_ROUND, currentRound);
        return true;
    }
}
