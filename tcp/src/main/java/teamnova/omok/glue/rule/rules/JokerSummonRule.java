package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
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
 * 조커 돌 소환: 전체 턴 종료 시 두 턴마다 빈 칸에 조커 돌을 생성한다.
 */
public final class JokerSummonRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.JOKER_SUMMON,
        0
    );

    private static final String LAST_ROUND_KEY = "rule:jokerSummon:lastRound";

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null) {
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

        TurnSnapshot snapshot = runtime.turnSnapshot();
        if (snapshot == null) {
            snapshot = services.turnService().snapshot(stateContext.turns());
        }
        if (snapshot == null) {
            return;
        }
        int roundNumber = Math.max(0, snapshot.roundNumber());
        if (roundNumber <= 0 || roundNumber % 2 != 0) {
            return;
        }
        Object lastRound = access.getRuleData(LAST_ROUND_KEY);
        if (lastRound instanceof Integer previous && previous == roundNumber) {
            return;
        }

        GameSessionBoardAccess board = stateContext.board();
        int width = board.width();
        int height = board.height();
        List<Integer> empties = new ArrayList<>();
        int total = width * height;
        for (int index = 0; index < total; index++) {
            if (board.stoneAt(index % width, index / width) == Stone.EMPTY) {
                empties.add(index);
            }
        }
        if (empties.isEmpty()) {
            access.putRuleData(LAST_ROUND_KEY, roundNumber);
            return;
        }

        int pick = empties.get(ThreadLocalRandom.current().nextInt(empties.size()));
        int x = pick % width;
        int y = pick / width;
        StonePlacementMetadata metadata = StonePlacementMetadata.forRule(snapshot, -1, null);
        services.boardService().setStone(board, x, y, Stone.JOKER, metadata);
        byte[] boardSnapshot = services.boardService().snapshot(board);
        runtime.contextService().postGame().queueBoardSnapshot(
            stateContext,
            new BoardSnapshotUpdate(boardSnapshot, System.currentTimeMillis())
        );
        access.putRuleData(LAST_ROUND_KEY, roundNumber);
    }
}
