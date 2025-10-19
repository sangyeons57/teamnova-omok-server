package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RulesContext;

public class EveryFiveTurnBlockerRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.FIVE_TURN_RANDOM_BLOCKER,
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
        System.out.println("[RULE_LOG] EveryFiveTurnBlockerRule invoked");
        GameSessionRuleAccess ruleStore = context.getSession();

        GameSessionStateContext stateContext = context.stateContext();
        GameSessionServices services = context.services();
        if (ruleStore == null || stateContext == null || services == null) {
            return;
        }
        GameSessionTurnAccess turnStore = context.getSession();
        int completedTurns = Math.max(0, turnStore.actionNumber() - 1);
        if (completedTurns <= 0 || completedTurns % 5 != 0) {
            return;
        }
        Object lastTrigger = context.getData(LAST_TRIGGER_KEY);
        if (lastTrigger instanceof Integer lastTurn && lastTurn == completedTurns) {
            return;
        }

        GameSessionBoardAccess boardStore = context.getSession();
        int width = boardStore.width();
        int height = boardStore.height();
        int totalCells = width * height;

        Map<Integer, List<Integer>> stonesByPlayer = new ConcurrentHashMap<>();
        for (int index = 0; index < totalCells; index++) {
            int x = index % width;
            int y = index / width;
            Stone stone = boardStore.stoneAt(x, y);
            if (!stone.isPlayerStone()) {
                continue;
            }
            int playerIndex = stone.code();
            stonesByPlayer.computeIfAbsent(playerIndex, ignored -> new ArrayList<>()).add(index);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        GameSessionParticipantsAccess participantsAccess = context.getSession();
        List<String> userIds = participantsAccess.getUserIds();
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
            services.boardService().setStone(boardStore, x, y, Stone.BLOCKER);
            mutated = true;
        }

        if (mutated) {
            System.out.println("[RULE_LOG] EveryFiveTurnBlockerRule placed blockers for players present");
            byte[] snapshot = services.boardService().snapshot(boardStore);
            stateContext.pendingBoardSnapshot(new BoardSnapshotUpdate(
                context.getSession(),
                snapshot,
                System.currentTimeMillis()
            ));
        } else {
            System.out.println("[RULE_LOG] EveryFiveTurnBlockerRule no placement this turn");
        }

        context.putData(LAST_TRIGGER_KEY, completedTurns);
    }
}
