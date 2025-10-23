package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.Stone;

/**
 * 바둑: 직교 방향으로 자유(빈칸)가 없는 돌은 턴 종료 시 제거한다.
 * 단순화를 위해 연결 그룹 대신 각 돌의 개별 자유만 검사한다.
 */
public class GoCaptureRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.GO_CAPTURE,
        1_900
    );

    @Override
    public RuleMetadata getMetadata() { return METADATA; }

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
        int w = board.width();
        int h = board.height();
        int removed = 0;
        // We'll mark removals in a boolean array to avoid interference during scanning
        boolean[] toRemove = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                Stone s = board.stoneAt(x, y);
                if (!s.isPlayerStone()) continue;
                boolean hasLiberty = false;
                if (x + 1 < w && board.stoneAt(x + 1, y) == Stone.EMPTY) hasLiberty = true;
                else if (x - 1 >= 0 && board.stoneAt(x - 1, y) == Stone.EMPTY) hasLiberty = true;
                else if (y + 1 < h && board.stoneAt(x, y + 1) == Stone.EMPTY) hasLiberty = true;
                else if (y - 1 >= 0 && board.stoneAt(x, y - 1) == Stone.EMPTY) hasLiberty = true;
                if (!hasLiberty) toRemove[idx] = true;
            }
        }
        for (int i = 0; i < toRemove.length; i++) {
            if (toRemove[i]) {
                int x = i % w; int y = i / w;
                services.boardService().setStone(board, x, y, Stone.EMPTY, null);
                removed++;
            }
        }
        if (removed > 0) {
            System.out.println("[RULE_LOG] GoCaptureRule removed " + removed + " stones (no liberties)");
            byte[] boardSnapshot = services.boardService().snapshot(board);
            runtime.contextService().postGame().queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(boardSnapshot, System.currentTimeMillis()));
        }
    }
}
