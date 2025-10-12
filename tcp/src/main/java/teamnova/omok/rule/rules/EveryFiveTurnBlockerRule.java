package teamnova.omok.rule.rules;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.rule.Rule;
import teamnova.omok.rule.RuleId;
import teamnova.omok.rule.RuleMetadata;
import teamnova.omok.rule.RuleType;
import teamnova.omok.rule.RulesContext;
import teamnova.omok.service.dto.BoardSnapshotUpdate;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.store.BoardStore;
import teamnova.omok.store.GameSession;
import teamnova.omok.store.Stone;
import teamnova.omok.store.TurnStore;

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
        TurnStore turnStore = session.getTurnStore();
        int completedTurns = Math.max(0, turnStore.getTurnNumber() - 1);
        if (completedTurns <= 0 || completedTurns % 5 != 0) {
            return;
        }
        Object lastTrigger = context.getData(LAST_TRIGGER_KEY);
        if (lastTrigger instanceof Integer lastTurn && lastTurn == completedTurns) {
            return;
        }

        BoardStore boardStore = session.getBoardStore();
        int width = boardStore.width();
        int height = boardStore.height();
        int totalCells = width * height;

        Map<Integer, List<Integer>> stonesByPlayer = new ConcurrentHashMap<>();
        for (int index = 0; index < totalCells; index++) {
            Stone stone = Stone.fromByte(boardStore.get(index));
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
            stateContext.boardService().setStone(boardStore, x, y, Stone.BLOCKER);
            mutated = true;
        }

        if (mutated) {
            byte[] snapshot = stateContext.boardService().snapshot(boardStore);
            stateContext.pendingBoardSnapshot(new BoardSnapshotUpdate(
                session,
                snapshot,
                System.currentTimeMillis()
            ));
        }

        context.putData(LAST_TRIGGER_KEY, completedTurns);
    }
}
