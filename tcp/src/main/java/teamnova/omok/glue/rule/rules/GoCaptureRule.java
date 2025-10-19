package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.Stone;

/**
 * Simplified Go-like capture rule: any individual stone that has no orthogonal liberty
 * (adjacent EMPTY) is removed at the end of each turn. Group liberties are not computed for
 * simplicity; this is a per-stone check.
 */
public class GoCaptureRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.GO_CAPTURE,
        0
    );

    @Override
    public RuleMetadata getMetadata() { return METADATA; }

    @Override
    public void invoke(RulesContext context) {
        if (context == null) return;
        GameSessionStateContext stateContext = context.stateContext();
        GameSessionServices services = context.services();
        if (stateContext == null || services == null) return;

        GameSessionBoardAccess board = context.getSession();
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
                services.boardService().setStone(board, x, y, Stone.EMPTY);
                removed++;
            }
        }
        if (removed > 0) {
            System.out.println("[RULE_LOG] GoCaptureRule removed " + removed + " stones (no liberties)");
            byte[] snapshot = services.boardService().snapshot(board);
            stateContext.pendingBoardSnapshot(new BoardSnapshotUpdate(context.getSession(), snapshot, System.currentTimeMillis()));
        }
    }
}
