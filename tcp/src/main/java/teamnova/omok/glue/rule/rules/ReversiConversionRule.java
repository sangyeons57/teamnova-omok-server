package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.List;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;

/**
 * 리버스: 리버스(오셀로) 방식으로 양쪽 끝 색이 동일한 줄을 변환한다.
 * 호출 시점: 돌이 배치된 후.
 */
public final class ReversiConversionRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.REVERSI_CONVERSION,
        700
    );

    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0},
        {0, 1}, {0, -1},
        {1, 1}, {-1, -1},
        {1, -1}, {-1, 1}
    };

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        if (runtime == null || runtime.triggerKind() != RuleTriggerKind.POST_PLACEMENT) {
            return;
        }
        GameSessionStateContext stateContext = runtime.stateContext();
        GameSessionServices services = runtime.services();
        if (stateContext == null || services == null) {
            return;
        }

        TurnPersonalFrame frame = runtime.contextService().turn().currentPersonalTurn(stateContext);
        if (frame == null || !frame.hasActiveMove()) {
            return;
        }
        Stone placedStone = frame.stone();
        if (placedStone == null || !placedStone.isPlayerStone()) {
            return;
        }
        GameSessionBoardAccess board = stateContext.board();
        int x = frame.x();
        int y = frame.y();

        boolean flipped = false;
        int actingPlayerIndex = stateContext.participants().playerIndexOf(frame.userId());
        StonePlacementMetadata metadata = buildMetadata(runtime, frame, actingPlayerIndex);

        for (int[] dir : DIRECTIONS) {
            List<int[]> path = collectFlippablePath(board, x, y, dir[0], dir[1], placedStone);
            if (path.isEmpty()) {
                continue;
            }
            for (int[] cell : path) {
                services.boardService().setStone(board, cell[0], cell[1], placedStone, metadata);
            }
            flipped = true;
        }

        if (flipped) {
            byte[] snapshot = services.boardService().snapshot(board);
            runtime.contextService()
                .postGame()
                .queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(snapshot, System.currentTimeMillis()));
        }
    }

    private StonePlacementMetadata buildMetadata(RuleRuntimeContext runtime,
                                                 TurnPersonalFrame frame,
                                                 int actingPlayerIndex) {
        if (frame.currentSnapshot() != null) {
            return StonePlacementMetadata.forRule(
                frame.currentSnapshot(),
                actingPlayerIndex,
                frame.userId()
            );
        }
        return StonePlacementMetadata.systemGenerated();
    }

    private List<int[]> collectFlippablePath(GameSessionBoardAccess board,
                                             int startX,
                                             int startY,
                                             int dx,
                                             int dy,
                                             Stone placedStone) {
        List<int[]> path = new ArrayList<>();
        int x = startX + dx;
        int y = startY + dy;
        while (board.isWithinBounds(x, y)) {
            Stone current = board.stoneAt(x, y);
            if (current == Stone.EMPTY || current.isBlocking()) {
                return List.of();
            }
            if (current == placedStone) {
                return path;
            }
            if (!current.isPlayerStone() && !current.isWildcard()) {
                return List.of();
            }
            path.add(new int[]{x, y});
            x += dx;
            y += dy;
        }
        return List.of();
    }
}
