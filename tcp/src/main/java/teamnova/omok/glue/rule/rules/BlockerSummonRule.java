package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;

/**
 * 방해 돌 소환: 전체 턴 종료 시 플레이어 돌 중 하나를 골라 인접한 빈 칸에 방해돌을 배치한다.
 */
public final class BlockerSummonRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.BLOCKER_SUMMON,
        0
    );

    private static final String LAST_ROUND_KEY = "rule:blockerSummon:lastRound";

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null) {
            return;
        }
        if (runtime.triggerKind() != RuleTriggerKind.TURN_ROUND_COMPLETED) {
            return;
        }
        GameSessionStateContext stateContext = runtime.stateContext();
        GameSessionServices services = runtime.services();
        if (stateContext == null || services == null) {
            return;
        }

        TurnSnapshot snapshot = resolveSnapshot(runtime, stateContext, services);
        if (snapshot == null) {
            return;
        }
        int roundNumber = Math.max(0, snapshot.roundNumber());
        Object lastRound = access.getRuleData(LAST_ROUND_KEY);
        if (lastRound instanceof Integer previous && previous == roundNumber) {
            return;
        }

        GameSessionBoardAccess board = stateContext.board();
        List<int[]> occupied = collectPlayerStones(board);
        if (occupied.isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int[] origin = occupied.get(random.nextInt(occupied.size()));
        List<int[]> candidates = findEmptyNeighbors(board, origin[0], origin[1]);
        if (candidates.isEmpty()) {
            access.putRuleData(LAST_ROUND_KEY, roundNumber);
            return;
        }
        int[] target = candidates.get(random.nextInt(candidates.size()));

        StonePlacementMetadata metadata = StonePlacementMetadata.forRule(snapshot, -1, null);
        services.boardService().setStone(board, target[0], target[1], Stone.BLOCKER, metadata);
        byte[] boardSnapshot = services.boardService().snapshot(board);
        runtime.contextService().postGame().queueBoardSnapshot(
            stateContext,
            new BoardSnapshotUpdate(boardSnapshot, System.currentTimeMillis())
        );
        access.putRuleData(LAST_ROUND_KEY, roundNumber);
    }

    private TurnSnapshot resolveSnapshot(RuleRuntimeContext runtime,
                                                         GameSessionStateContext stateContext,
                                                         GameSessionServices services) {
        TurnSnapshot snapshot = runtime.turnSnapshot();
        if (snapshot != null) {
            return snapshot;
        }
        return services.turnService().snapshot(stateContext.turns());
    }

    private List<int[]> collectPlayerStones(GameSessionBoardAccess board) {
        int width = board.width();
        int height = board.height();
        List<int[]> stones = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Stone stone = board.stoneAt(x, y);
                if (stone.isPlayerStone()) {
                    stones.add(new int[]{x, y});
                }
            }
        }
        return stones;
    }

    private List<int[]> findEmptyNeighbors(GameSessionBoardAccess board,
                                          int x,
                                          int y) {
        int[][] offsets = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        List<int[]> candidates = new ArrayList<>();
        for (int[] offset : offsets) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (nx < 0 || ny < 0 || nx >= board.width() || ny >= board.height()) {
                continue;
            }
            if (board.stoneAt(nx, ny) == Stone.EMPTY) {
                candidates.add(new int[]{nx, ny});
            }
        }
        return candidates;
    }
}
