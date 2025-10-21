package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
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
 * 이동: 무작위로 선택된 돌을 막히지 않은 한 칸 방향으로 이동시킨다.
 * 호출 시점: 전체 턴 종료 시.
 */
public final class RandomMoveRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.RANDOM_MOVE,
        1_600
    );

    private static final String LAST_ROUND_KEY = "rule:randomMove:lastRound";

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess acces, RuleRuntimeContext runtime) {
        if (acces == null || runtime == null || runtime.triggerKind() != RuleTriggerKind.TURN_ROUND_COMPLETED) {
            return;
        }
        GameSessionStateContext stateContext = runtime.stateContext();
        GameSessionServices services = runtime.services();
        if (stateContext == null || services == null) {
            return;
        }
        TurnSnapshot snapshot = runtime.turnSnapshot();
        if (snapshot == null) {
            snapshot = services.turnService().snapshot(stateContext.turns());
        }
        int roundNumber = snapshot != null
            ? snapshot.roundNumber()
            : stateContext.turns().counters().roundNumber();
        Object last = acces.getRuleData(LAST_ROUND_KEY);
        if (last instanceof Integer processed && processed == roundNumber) {
            return;
        }

        GameSessionBoardAccess board = stateContext.board();
        List<int[]> stones = collectPlayerStones(board);
        if (stones.isEmpty()) {
            acces.putRuleData(LAST_ROUND_KEY, roundNumber);
            return;
        }
        Collections.shuffle(stones, ThreadLocalRandom.current());
        GameSessionParticipantsAccess participants = stateContext.participants();
        int attempts = Math.min(participants.getUserIds().size(), stones.size());

        boolean moved = false;
        StonePlacementMetadata metadata = buildMetadata(snapshot);
        for (int i = 0; i < attempts; i++) {
            int[] pos = stones.get(i);
            moved |= tryMove(board, services, pos[0], pos[1], metadata);
        }

        if (moved) {
            byte[] bytes = services.boardService().snapshot(board);
            runtime.contextService()
                .postGame()
                .queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(bytes, System.currentTimeMillis()));
        }
        acces.putRuleData(LAST_ROUND_KEY, roundNumber);
    }

    private List<int[]> collectPlayerStones(GameSessionBoardAccess board) {
        List<int[]> result = new ArrayList<>();
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Stone stone = board.stoneAt(x, y);
                if (stone.isPlayerStone()) {
                    result.add(new int[]{x, y});
                }
            }
        }
        return result;
    }

    private boolean tryMove(GameSessionBoardAccess board,
                            GameSessionServices services,
                            int x,
                            int y,
                            StonePlacementMetadata metadata) {
        Stone stone = board.stoneAt(x, y);
        if (!stone.isPlayerStone()) {
            return false;
        }
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        List<int[]> candidates = new ArrayList<>();
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (!board.isWithinBounds(nx, ny)) {
                continue;
            }
            if (board.isEmpty(nx, ny)) {
                candidates.add(new int[]{nx, ny});
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        int[] pick = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        services.boardService().setStone(board, x, y, Stone.EMPTY, null);
        services.boardService().setStone(board, pick[0], pick[1], stone, metadata);
        return true;
    }

    private StonePlacementMetadata buildMetadata(TurnSnapshot snapshot) {
        if (snapshot != null) {
            return StonePlacementMetadata.forRule(snapshot, -1, null);
        }
        return StonePlacementMetadata.systemGenerated();
    }
}
