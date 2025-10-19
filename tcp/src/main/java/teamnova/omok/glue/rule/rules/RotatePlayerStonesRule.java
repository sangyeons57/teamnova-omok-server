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
 * Each turn, rotate player stones: 1->2, 2->3, 3->4, 4->1.
 */
public class RotatePlayerStonesRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.ROTATE_STONES,
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
        int changed = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Stone s = board.stoneAt(x, y);
                Stone ns = s;
                if (s == Stone.PLAYER1) ns = Stone.PLAYER2;
                else if (s == Stone.PLAYER2) ns = Stone.PLAYER3;
                else if (s == Stone.PLAYER3) ns = Stone.PLAYER4;
                else if (s == Stone.PLAYER4) ns = Stone.PLAYER1;
                if (ns != s) {
                    services.boardService().setStone(board, x, y, ns);
                    changed++;
                }
            }
        }
        if (changed > 0) {
            System.out.println("[RULE_LOG] RotatePlayerStonesRule rotated " + changed + " stones");
            byte[] snapshot = services.boardService().snapshot(board);
            stateContext.pendingBoardSnapshot(new BoardSnapshotUpdate(context.getSession(), snapshot, System.currentTimeMillis()));
        }
    }
}
