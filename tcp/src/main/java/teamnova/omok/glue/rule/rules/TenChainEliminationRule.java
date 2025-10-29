package teamnova.omok.glue.rule.rules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.rule.api.OutcomeResolution;
import teamnova.omok.glue.rule.api.OutcomeRule;
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
public final class TenChainEliminationRule implements Rule, OutcomeRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(TenChainEliminationRule.class);

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
        // Primary behaviour executed via resolveOutcome.
    }

    @Override
    public Optional<OutcomeResolution> resolveOutcome(GameSessionRuleAccess access,
                                                      RuleRuntimeContext runtime,
                                                      OutcomeResolution currentOutcome) {
        if (access == null || runtime == null || runtime.stateContext() == null
            || runtime.triggerKind() != RuleTriggerKind.OUTCOME_EVALUATION
            || currentOutcome == null) {
            return Optional.empty();
        }
        int turnNumber = runtime.turnSnapshot() != null ? runtime.turnSnapshot().turnNumber() : -1;
        Integer lastTurn = (Integer) access.getRuleData(RuleDataKeys.TEN_CHAIN_LAST_TURN);
        if (lastTurn != null && lastTurn == turnNumber) {
            return Optional.empty();
        }
        GameSessionBoardAccess board = runtime.stateContext().board();
        if (board == null) {
            return Optional.empty();
        }
        List<String> participants = runtime.stateContext().participants().getUserIds();
        if (participants == null || participants.isEmpty()) {
            LOGGER.debug("TenChainElimination: no participants available for turn={}", turnNumber);
            return Optional.empty();
        }
        Map<String, PlayerResult> assignments = new LinkedHashMap<>();
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
                assignments.put(userId, PlayerResult.LOSS);
                LOGGER.info(
                    "TenChainElimination: detected 10-chain for user={} stone={} turn={}",
                    userId,
                    playerStone,
                    turnNumber
                );
            }
        }
        if (assignments.isEmpty()) {
            return Optional.empty();
        }
        for (String userId : participants) {
            if (!assignments.containsKey(userId)) {
                assignments.put(userId, PlayerResult.WIN);
            }
        }
        access.putRuleData(RuleDataKeys.TEN_CHAIN_LAST_TURN, turnNumber);
        LOGGER.info(
            "TenChainElimination: finalizing outcomes turn={} assignments={}",
            turnNumber,
            assignments
        );
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
