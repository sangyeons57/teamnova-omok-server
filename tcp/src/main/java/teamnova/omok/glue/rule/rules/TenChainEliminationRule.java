package teamnova.omok.glue.rule.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.runtime.RuleDataKeys;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 10수: 같은 종류의 돌이 10개 연속이면 해당 플레이어를 패배 처리한다.(연결되어있으면)
 * 호출 시점: 턴 종료 시.
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
        for (String userId : participants) {
            int index = runtime.stateContext().participants().playerIndexOf(userId);
            if (index < 0) {
                continue;
            }
            Stone playerStone = Stone.fromPlayerOrder(index);
            if (playerStone == Stone.EMPTY) {
                continue;
            }
            if (hasSequence(board, playerStone, 10)) {
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

    private boolean hasSequence(GameSessionBoardAccess board, Stone stone, int targetLength) {
        int width = board.width();
        int height = board.height();
        int[][] directions = {
            {1, 0},
            {0, 1},
            {1, 1},
            {1, -1}
        };
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Stone current = board.stoneAt(x, y);
                if (current == null || !current.countsForPlayerSequence(stone)) {
                    continue;
                }
                for (int[] dir : directions) {
                    int count = 1;
                    count += countDirection(board, x, y, dir[0], dir[1], stone);
                    count += countDirection(board, x, y, -dir[0], -dir[1], stone);
                    if (count >= targetLength) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int countDirection(GameSessionBoardAccess board,
                               int startX,
                               int startY,
                               int dx,
                               int dy,
                               Stone stone) {
        int count = 0;
        int x = startX + dx;
        int y = startY + dy;
        while (board.isWithinBounds(x, y)) {
            Stone current = board.stoneAt(x, y);
            if (current == null || !current.countsForPlayerSequence(stone)) {
                break;
            }
            count++;
            x += dx;
            y += dy;
        }
        return count;
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
