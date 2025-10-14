package teamnova.omok.domain.rule.model.rules;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.domain.rule.model.Rule;
import teamnova.omok.domain.rule.model.RuleId;
import teamnova.omok.domain.rule.model.RuleMetadata;
import teamnova.omok.domain.rule.model.RuleType;
import teamnova.omok.domain.rule.RulesContext;
import teamnova.omok.service.dto.BoardSnapshotUpdate;
import teamnova.omok.domain.session.game.entity.board.BoardReadable;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.stone.Stone;
import teamnova.omok.domain.session.game.entity.turn.Turn;

public class EveryFiveTurnBlockerRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.FIVE_TURN_RANDOM_BLOCKER,
        EnumSet.of(RuleType.TURN_FINALIZING),
        0
    );

    private static final String LAST_TRIGGER_KEY = "fiveTurnBlocker:lastTurn";

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(RulesContext context) {
        if (context == null) {
            return;
        }
        GameSession session = context.getSession();
        GameSessionStateContext stateContext = context.stateContext();
        if (session == null || stateContext == null) {
            return;
        }
        Turn turn = session.getTurnStore();
        int completedTurns = Math.max(0, turn.getTurnNumber() - 1);
        if (completedTurns <= 0 || completedTurns % 5 != 0) {
            return;
        }
        Object lastTrigger = context.getData(LAST_TRIGGER_KEY);
        if (lastTrigger instanceof Integer lastTurn && lastTurn == completedTurns) {
            return;
        }

        BoardReadable board = session.getBoard();
        int width = board.width();
        int height = board.height();
        int totalCells = width * height;

        Map<Integer, List<Integer>> stonesByPlayer = new ConcurrentHashMap<>();
        for (int index = 0; index < totalCells; index++) {
            Stone stone = Stone.fromByte(board.get(index));
            if (!stone.isPlayerStone()) {
                continue;
            }
            int playerIndex = stone.code();
            stonesByPlayer.computeIfAbsent(playerIndex, ignored -> new ArrayList<>()).add(index);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> userIds = session.getUserIds();
        boolean mutated = false;
        for (int playerIndex = 0; playerIndex < userIds.size(); playerIndex++) {
            Stone playerStone = Stone.fromPlayerOrder(playerIndex);
            if (!playerStone.isPlayerStone()) {
                continue;
            }
            List<Integer> occupied = stonesByPlayer.get(playerStone.code());
            if (occupied == null || occupied.isEmpty()) {
                continue;
            }
            int cellIndex = occupied.get(random.nextInt(occupied.size()));
            int x = cellIndex % width;
            int y = cellIndex / width;
            session.setStone(x, y, Stone.BLOCKER);
            mutated = true;
        }

        if (mutated) {
            byte[] snapshot = session.getBoard().snapshot();
            stateContext.pendingBoardSnapshot(new BoardSnapshotUpdate(
                session,
                snapshot,
                System.currentTimeMillis()
            ));
        }

        context.putData(LAST_TRIGGER_KEY, completedTurns);
    }
}
