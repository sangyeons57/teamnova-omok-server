package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.services.RuleTurnStateView;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.Stone;

/**
 * Each turn, for each player that has at least one stone, spawn one BLOCKER
 * adjacent to a randomly chosen stone of that player if an empty neighbor exists.
 */
public class PerTurnAdjacentBlockerRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.PER_TURN_ADJACENT_BLOCKER,
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
        if (completedTurns <= 0) return; // start after first move completes

        GameSessionBoardAccess board = stateContext.board();
        int w = board.width();
        int h = board.height();
        int total = w * h;

        Map<Integer, List<Integer>> byPlayer = new HashMap<>();
        for (int i = 0; i < total; i++) {
            Stone s = board.stoneAt(i % w, i / w);
            if (!s.isPlayerStone()) continue;
            byPlayer.computeIfAbsent((int) s.code(), k -> new ArrayList<>()).add(i);
        }

        GameSessionParticipantsAccess participants = stateContext.participants();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int placed = 0;
        for (int p = 0; p < participants.getUserIds().size(); p++) {
            Stone ps = Stone.fromPlayerOrder(p);
            List<Integer> stones = byPlayer.get((int) ps.code());
            if (stones == null || stones.isEmpty()) continue;
            int pickedIndex = stones.get(rnd.nextInt(stones.size()));
            int x = pickedIndex % w;
            int y = pickedIndex / w;
            int[][] dirs = new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
            List<int[]> candidates = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (board.stoneAt(nx, ny) == Stone.EMPTY) {
                    candidates.add(new int[]{nx, ny});
                }
            }
            if (!candidates.isEmpty()) {
                int[] c = candidates.get(rnd.nextInt(candidates.size()));
                StonePlacementMetadata metadata = turnSnapshot != null
                    ? StonePlacementMetadata.forRule(turnSnapshot, -1, null)
                    : StonePlacementMetadata.systemGenerated();
                services.boardService().setStone(board, c[0], c[1], Stone.BLOCKER, metadata);
                placed++;
            }
        }
        if (placed > 0) {
            System.out.println("[RULE_LOG] PerTurnAdjacentBlockerRule placed " + placed + " blockers");
            byte[] boardSnapshot = services.boardService().snapshot(board);
            runtime.contextService().postGame().queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(boardSnapshot, System.currentTimeMillis()));
        } else {
            System.out.println("[RULE_LOG] PerTurnAdjacentBlockerRule no placement this turn");
        }
    }
}
