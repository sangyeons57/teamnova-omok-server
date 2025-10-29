package teamnova.omok.glue.rule.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.board.ConnectedGroup;
import teamnova.omok.glue.game.session.model.board.Connectivity;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.runtime.RuleDataKeys;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 10수: 같은 종류의 돌이 10개 연속이면 해당 플레이어를 패배 처리한다.(연결되어있으면)
 * 호출 시점: 턴 종료 시.
 * 통과 (2025.10.29)
 */
public final class TenChainEliminationRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.TEN_CHAIN_ELIMINATION,
        800
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        if (context == null || runtime == null || runtime.stateContext() == null) {
            return;
        }
        if (runtime.triggerKind() != RuleTriggerKind.OUTCOME_EVALUATION) {
            return;
        }
        GameSessionBoardAccess board = runtime.stateContext().board();
        if (board == null) {
            return;
        }
        List<String> participants = runtime.stateContext().participants().getUserIds();
        if (participants == null || participants.isEmpty()) {
            System.out.println("[TenChainElimination] skip: no participants");
            return;
        }

        int turnNumber = resolveTurnNumber(runtime);
        Integer lastTurn = (Integer) context.getRuleData(RuleDataKeys.TEN_CHAIN_LAST_TURN);
        if (lastTurn != null && Objects.equals(lastTurn, turnNumber)) {
            return;
        }

        Set<String> eliminated = new HashSet<>();
        List<ConnectedGroup> allGroups = runtime.services()
            .boardService()
            .connectedGroups(board, Connectivity.FOUR_WAY);
        for (String userId : participants) {
            int index = runtime.stateContext().participants().playerIndexOf(userId);
            if (index < 0) {
                continue;
            }
            Stone playerStone = Stone.fromPlayerOrder(index);
            if (playerStone == Stone.EMPTY) {
                continue;
            }
            boolean matched = allGroups.stream()
                .filter(group -> group.stone() == playerStone)
                .anyMatch(group -> group.size() >= 10);
            if (matched) {
                runtime.stateContext().outcomes().updateOutcome(userId, PlayerResult.LOSS);
                System.out.println(
                    "[TenChainElimination] detected 10-chain: user="
                        + userId + " stone=" + playerStone + " turn=" + turnNumber
                );
                eliminated.add(userId);
            }
        }
        if (eliminated.isEmpty()) {
            return;
        }
        for (String userId : participants) {
            if (!eliminated.contains(userId)) {
                runtime.stateContext().outcomes().updateOutcome(userId, PlayerResult.WIN);
            }
        }
        context.putRuleData(RuleDataKeys.TEN_CHAIN_LAST_TURN, turnNumber);
        System.out.println("[TenChainElimination] finalized outcomes for turn " + turnNumber);
    }

    private int resolveTurnNumber(RuleRuntimeContext runtime) {
        if (runtime.turnSnapshot() != null) {
            return runtime.turnSnapshot().turnNumber();
        }
        var counters = runtime.stateContext().turns().counters();
        if (counters != null) {
            return counters.actionNumber();
        }
        return -1;
    }
}
