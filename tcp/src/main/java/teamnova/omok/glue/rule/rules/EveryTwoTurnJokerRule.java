package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.services.RuleTurnStateView;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RulesContext;

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
    public void invoke(RulesContext context, RuleRuntimeContext runtime) {
        if (context == null || runtime == null) {
            return;
        }
        GameSessionStateContext stateContext = runtime.stateContext();
        GameSessionServices services = runtime.services();
        if (stateContext == null || services == null) {
            return;
        }

        RuleTurnStateView view = runtime.turnStateView();
        if (view == null) {
            view = RuleTurnStateView.capture(stateContext, services.turnService());
        }
        GameTurnService.TurnSnapshot turnSnapshot = view != null ? view.resolvedSnapshot() : null;

        GameSessionTurnAccess turn = stateContext.turns();
        int completedTurns = Math.max(0, turn.actionNumber() - 1);
        if (completedTurns <= 0 || completedTurns % 2 != 0) return;

        GameSessionBoardAccess board = stateContext.board();
        int w = board.width();
        int h = board.height();
        List<Integer> empties = new ArrayList<>();
        int total = w * h;
        for (int i = 0; i < total; i++) {
            if (board.stoneAt(i % w, i / w) == Stone.EMPTY) empties.add(i);
        }
        if (empties.isEmpty()) {
            System.out.println("[RULE_LOG] EveryTwoTurnJokerRule no empty cell");
            return;
        }
        int pick = empties.get(ThreadLocalRandom.current().nextInt(empties.size()));
        int x = pick % w;
        int y = pick / w;
        StonePlacementMetadata metadata = turnSnapshot != null
            ? StonePlacementMetadata.forRule(turnSnapshot, -1, null)
            : StonePlacementMetadata.systemGenerated();
        services.boardService().setStone(board, x, y, Stone.JOKER, metadata);
        System.out.println("[RULE_LOG] EveryTwoTurnJokerRule placed JOKER at (" + x + "," + y + ")");
        byte[] boardSnapshot = services.boardService().snapshot(board);
        runtime.contextService().postGame().queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(stateContext.session(), boardSnapshot, System.currentTimeMillis()));
    }
}
