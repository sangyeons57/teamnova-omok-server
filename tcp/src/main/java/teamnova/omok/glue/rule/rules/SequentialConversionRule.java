package teamnova.omok.glue.rule.rules;

import java.util.List;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.Stone;

/**
 * 변환: 전체 턴 시작 시 플레이어 돌을 순서대로 다음 플레이어의 돌로 변환한다.
 */
public final class SequentialConversionRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.SEQUENTIAL_CONVERSION,
        200
    );

    @Override
    public RuleMetadata getMetadata() { return METADATA; }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        if (context == null || runtime == null) {
            return;
        }
        if (runtime.triggerKind() != RuleTriggerKind.TURN_START) {
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

        GameSessionBoardAccess board = stateContext.board();
        GameSessionParticipantsAccess participants = stateContext.participants();
        List<String> userOrder = participants.getUserIds();
        int w = board.width();
        int h = board.height();
        int changed = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Stone s = board.stoneAt(x, y);
                Stone ns = s;
                // Rotate only among the currently active players (2~4). Others stay as-is.
                int playerCount = (userOrder != null) ? Math.min(Math.max(userOrder.size(), 0), 4) : 0;
                if (playerCount >= 2 && s != null && s.isPlayerStone()) {
                    Stone[] ring = new Stone[] { Stone.PLAYER1, Stone.PLAYER2, Stone.PLAYER3, Stone.PLAYER4 };
                    int activeIdx = -1;
                    for (int i = 0; i < playerCount; i++) {
                        if (s == ring[i]) { activeIdx = i; break; }
                    }
                    if (activeIdx != -1) {
                        ns = ring[(activeIdx + 1) % playerCount];
                    }
                }
                if (ns != s) {
                    int playerIndex = ns.isPlayerStone() ? ns.code() : -1;
                    String userId = (playerIndex >= 0 && playerIndex < userOrder.size())
                        ? userOrder.get(playerIndex)
                        : null;
                    StonePlacementMetadata metadata = turnSnapshot != null
                        ? StonePlacementMetadata.forRule(turnSnapshot, playerIndex, userId)
                        : StonePlacementMetadata.systemGenerated();
                    services.boardService().setStone(board, x, y, ns, metadata);
                    changed++;
                }
            }
        }
        if (changed > 0) {
            System.out.println("[RULE_LOG] SequentialConversionRule rotated " + changed + " stones");
            byte[] boardSnapshot = services.boardService().snapshot(board);
            runtime.contextService().postGame().queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(boardSnapshot, System.currentTimeMillis()));
        }
    }
}
