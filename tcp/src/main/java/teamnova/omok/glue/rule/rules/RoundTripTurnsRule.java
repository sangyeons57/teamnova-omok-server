package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.api.TurnOrderAdjustment;
import teamnova.omok.glue.rule.api.TurnOrderRule;
import teamnova.omok.glue.rule.runtime.RuleDataKeys;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 왕복: 플레이어 턴 순서가 왕복(정방향→역방향)으로 진행되도록 한다.
 * 호출 시점: 라운드 종료 시 다음 라운드를 위해 순서를 재설정.
 * ㅐ*
 * 통과(2025.10.31)
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
    public TurnOrderAdjustment adjustTurnOrder(GameSessionRuleAccess access,
                                               RuleRuntimeContext runtime,
                                               TurnOrderAdjustment current) {
        if (access == null || runtime == null
            || current == null
            || runtime.triggerKind() != RuleTriggerKind.TURN_ROUND_COMPLETED
            || runtime.turnSnapshot() == null) {
            return current;
        }
        int currentRound = runtime.turnSnapshot().counters().roundNumber();
        Integer lastRound = (Integer) access.getRuleData(RuleDataKeys.ROUND_TRIP_LAST_ROUND);
        if (lastRound != null && lastRound == currentRound) {
            return current; // already applied for this round
        }
        if (runtime.stateContext() == null || runtime.stateContext().turns() == null) {
            return current;
        }
        List<String> currentOrder = current.order();
        if (currentOrder.size() <= 1) {
            access.putRuleData(RuleDataKeys.ROUND_TRIP_LAST_ROUND, currentRound);
            return current;
        }
        List<String> nextOrder = new ArrayList<>(currentOrder);
        Collections.reverse(nextOrder);
        // NOTE: 매 라운드마다 순서를 뒤집어 왕복을 만들고, 고정 참가자 인덱스는 다른 계층에서 사용한다.
        access.putRuleData(RuleDataKeys.ROUND_TRIP_LAST_ROUND, currentRound);
        return current.withOrder(nextOrder);
    }
}
