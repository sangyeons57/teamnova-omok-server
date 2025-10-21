package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;

/**
 * 돌 변환: 전체 턴(라운드)이 5의 배수로 종료될 때마다 각 플레이어의 돌을 방해돌로 바꾼다.
 */
public class StoneConversionRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.STONE_CONVERSION,
        0
    );

    private static final String LAST_TRIGGER_KEY = "rule:stoneConversion:lastCompletedTurns";

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        if (context == null || runtime == null) {
            return;
        }
        if (runtime.triggerKind() != RuleTriggerKind.TURN_ROUND_COMPLETED) {
            return;
        }
        GameSessionStateContext stateContext = runtime.stateContext();
        GameSessionServices services = runtime.services();
        if (stateContext == null || services == null) {
            return;
        }

        TurnSnapshot turnSnapshot = runtime.turnSnapshot();
        if (turnSnapshot == null) {
            turnSnapshot = services.turnService().snapshot(stateContext.turns());
        }

        if (turnSnapshot == null) {
            return;
        }
        int roundNumber = Math.max(0, turnSnapshot.roundNumber());
        if (roundNumber <= 0 || roundNumber % 5 != 0) {
            return;
        }
        Object lastTrigger = context.getRuleData(LAST_TRIGGER_KEY);
        if (lastTrigger instanceof Integer lastRound && lastRound == roundNumber) {
            return;
        }

        GameSessionBoardAccess boardStore = stateContext.board();
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
        GameSessionParticipantsAccess participantsAccess = stateContext.participants();
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
            StonePlacementMetadata metadata = turnSnapshot != null
                ? StonePlacementMetadata.forRule(turnSnapshot, -1, null)
                : StonePlacementMetadata.systemGenerated();
            services.boardService().setStone(boardStore, x, y, Stone.BLOCKER, metadata);
            mutated = true;
        }

        if (mutated) {
            System.out.println("[RULE_LOG] StoneConversionRule placed blockers for players present");
            byte[] snapshot = services.boardService().snapshot(boardStore);
            runtime.contextService().postGame().queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(
                snapshot,
                System.currentTimeMillis()
            ));
        } else {
            System.out.println("[RULE_LOG] StoneConversionRule no placement this turn");
        }

        context.putRuleData(LAST_TRIGGER_KEY, roundNumber);
    }
}
