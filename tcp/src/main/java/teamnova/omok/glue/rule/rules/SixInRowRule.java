package teamnova.omok.glue.rule.rules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.api.OutcomeResolution;
import teamnova.omok.glue.rule.api.OutcomeRule;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleDataKeys;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 6목: 승리 조건을 5목에서 6목으로 확장한다. (5목 6목 둘다 허용)
 * 호출 시점: 게임 시작 시.
 * 통과(2025.10.29)
 */
public final class SixInRowRule implements Rule, OutcomeRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.SIX_IN_ROW,
        400
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        // Primary behaviour executed via resolveOutcome.
    }

    @Override
    public Optional<OutcomeResolution> resolveOutcome(GameSessionRuleAccess access,
                                                      RuleRuntimeContext runtime) {
        if (access == null || runtime == null || runtime.stateContext() == null
            || runtime.triggerKind() != RuleTriggerKind.OUTCOME_EVALUATION) {
            return Optional.empty();
        }
        int turnNumber = runtime.turnSnapshot() != null ? runtime.turnSnapshot().turnNumber() : -1;
        Integer lastTurn = (Integer) access.getRuleData(RuleDataKeys.SIX_IN_ROW_LAST_TURN);
        if (lastTurn != null && lastTurn == turnNumber) {
            return Optional.empty();
        }
        GameSessionBoardAccess board = runtime.stateContext().board();
        if (board == null) {
            return Optional.empty();
        }
        Map<String, PlayerResult> assignments = new LinkedHashMap<>();
        List<String> participants = runtime.stateContext().participants().getUserIds();
        for (String userId : participants) {
            int index = runtime.stateContext().participants().playerIndexOf(userId);
            if (index < 0) {
                continue;
            }
            Stone playerStone = Stone.fromPlayerOrder(index);
            if (playerStone == Stone.EMPTY) {
                continue;
            }
            if (hasSequence(board, playerStone, 6)) {
                assignments.put(userId, PlayerResult.WIN);
            }
        }
        if (assignments.isEmpty()) {
            return Optional.empty();
        }
        access.putRuleData(RuleDataKeys.SIX_IN_ROW_LAST_TURN, turnNumber);
        return Optional.of(OutcomeResolution.of(assignments, true));
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
}
