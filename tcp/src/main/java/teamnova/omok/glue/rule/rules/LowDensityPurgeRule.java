package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.List;

import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.rule.api.BoardSweepRule;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleDataKeys;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 뭉쳐야 산다: 5턴마다 주변에 돌이 가장 적은 돌들을 제거한다.
 * 호출 시점: 전체 턴 종료 시.
 */
public final class LowDensityPurgeRule implements Rule, BoardSweepRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.LOW_DENSITY_PURGE,
        1_700
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Primary behaviour executed via sweepBoard.
    }

    @Override
    public void sweepBoard(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null || runtime.turnSnapshot() == null) {
            return;
        }
        int turnNumber = runtime.turnSnapshot().turnNumber();
        if (turnNumber <= 0 || turnNumber % 5 != 0) {
            return;
        }
        Integer lastProcessed = (Integer) access.getRuleData(RuleDataKeys.LOW_DENSITY_LAST_TURN);
        if (lastProcessed != null && lastProcessed == turnNumber) {
            return;
        }
        GameSessionBoardAccess board = runtime.stateContext().board();
        GameBoardService boardService = runtime.services().boardService();
        if (board == null || boardService == null) {
            return;
        }
        int width = board.width();
        int height = board.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        int total = width * height;
        int[] neighborCounts = new int[total];
        for (int i = 0; i < neighborCounts.length; i++) {
            neighborCounts[i] = -1;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                Stone stone = board.stoneAt(x, y);
                if (stone == null || stone == Stone.EMPTY || stone.isBlocking()) {
                    continue;
                }
                int count = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                            continue;
                        }
                        Stone neighbor = board.stoneAt(nx, ny);
                        if (neighbor != null && neighbor != Stone.EMPTY && !neighbor.isBlocking()) {
                            count++;
                        }
                    }
                }
                neighborCounts[index] = count;
            }
        }
        int minCount = Integer.MAX_VALUE;
        for (int count : neighborCounts) {
            if (count >= 0 && count < minCount) {
                minCount = count;
            }
        }
        if (minCount == Integer.MAX_VALUE) {
            access.putRuleData(RuleDataKeys.LOW_DENSITY_LAST_TURN, turnNumber);
            return;
        }
        List<Integer> purgeIndices = new ArrayList<>();
        for (int index = 0; index < neighborCounts.length; index++) {
            if (neighborCounts[index] < 0) {
                continue;
            }
            if (neighborCounts[index] == minCount) {
                purgeIndices.add(index);
            }
        }
        if (purgeIndices.isEmpty()) {
            access.putRuleData(RuleDataKeys.LOW_DENSITY_LAST_TURN, turnNumber);
            return;
        }
        for (int index : purgeIndices) {
            int x = index % width;
            int y = index / width;
            boardService.setStone(board, x, y, Stone.EMPTY, null);
        }
        GameSessionMessenger messenger = runtime.services().messenger();
        if (messenger != null) {
            messenger.broadcastBoardSnapshot(runtime.stateContext().session());
        }
        access.putRuleData(RuleDataKeys.LOW_DENSITY_LAST_TURN, turnNumber);
    }
}
