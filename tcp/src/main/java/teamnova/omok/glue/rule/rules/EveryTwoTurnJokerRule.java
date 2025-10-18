package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.service.dto.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.BoardStore;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.TurnStore;

/**
 * Every two turns, spawn one JOKER stone at a random empty cell.
 */
public class EveryTwoTurnJokerRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.EVERY_TWO_TURN_JOKER,
        0
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(RulesContext context) {
        if (context == null) return;
        GameSession session = context.getSession();
        GameSessionStateContext stateContext = context.stateContext();
        GameSessionServices services = context.services();
        if (session == null || stateContext == null || services == null) return;

        TurnStore turn = session.getTurnStore();
        int completedTurns = Math.max(0, turn.getTurnNumber() - 1);
        if (completedTurns <= 0 || completedTurns % 2 != 0) return;

        BoardStore board = session.getBoardStore();
        int w = board.width();
        int h = board.height();
        List<Integer> empties = new ArrayList<>();
        int total = w * h;
        for (int i = 0; i < total; i++) {
            if (Stone.fromByte(board.get(i)) == Stone.EMPTY) empties.add(i);
        }
        if (empties.isEmpty()) {
            System.out.println("[RULE_LOG] EveryTwoTurnJokerRule no empty cell");
            return;
        }
        int pick = empties.get(ThreadLocalRandom.current().nextInt(empties.size()));
        int x = pick % w;
        int y = pick / w;
        services.boardService().setStone(board, x, y, Stone.JOKER);
        System.out.println("[RULE_LOG] EveryTwoTurnJokerRule placed JOKER at (" + x + "," + y + ")");
        byte[] snapshot = services.boardService().snapshot(board);
        stateContext.pendingBoardSnapshot(new BoardSnapshotUpdate(session, snapshot, System.currentTimeMillis()));
    }
}
