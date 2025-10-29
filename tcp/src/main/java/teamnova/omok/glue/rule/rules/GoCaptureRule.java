package teamnova.omok.glue.rule.rules;

import java.util.List;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.board.BoardPoint;
import teamnova.omok.glue.game.session.model.board.ConnectedGroup;
import teamnova.omok.glue.game.session.model.board.Connectivity;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 바둑: 직교 방향으로 자유(빈칸)가 없는 돌은 턴 종료 시 제거한다.
 */
public class GoCaptureRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.GO_CAPTURE,
        1_900
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null || runtime.triggerKind() != RuleTriggerKind.POST_PLACEMENT) {
            return;
        }
        GameSessionStateContext stateContext = runtime.stateContext();
        GameSessionServices services = runtime.services();
        if (stateContext == null || services == null) {
            return;
        }

        GameSessionBoardAccess board = stateContext.board();
        int width = board.width();
        int height = board.height();
        List<ConnectedGroup> groups = services.boardService().connectedGroups(board, Connectivity.FOUR_WAY);
        boolean[] toRemove = new boolean[width * height];
        int removed = 0;
        for (ConnectedGroup group : groups) {
            if (hasLiberty(board, group)) {
                continue;
            }
            for (BoardPoint point : group.points()) {
                int index = point.y() * width + point.x();
                toRemove[index] = true;
            }
        }
        for (int i = 0; i < toRemove.length; i++) {
            if (toRemove[i]) {
                int x = i % width;
                int y = i / width;
                services.boardService().setStone(board, x, y, Stone.EMPTY, null);
                removed++;
            }
        }
        if (removed > 0) {
            System.out.println("[RULE_LOG] GoCaptureRule removed " + removed + " stones (no liberties)");
            byte[] boardSnapshot = services.boardService().snapshot(board);
            runtime.contextService().postGame().queueBoardSnapshot(
                stateContext,
                new BoardSnapshotUpdate(boardSnapshot, System.currentTimeMillis())
            );
        }
    }

    private boolean hasLiberty(GameSessionBoardAccess board, ConnectedGroup group) {
        for (BoardPoint point : group.points()) {
            for (int[] offset : Connectivity.FOUR_WAY.offsets()) {
                int nx = point.x() + offset[0];
                int ny = point.y() + offset[1];
                if (board.isWithinBounds(nx, ny) && board.stoneAt(nx, ny) == Stone.EMPTY) {
                    return true;
                }
            }
        }
        return false;
    }
}
