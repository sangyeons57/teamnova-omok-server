package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;

/**
 * 방해 금지: 돌이 놓인 직후 판 전체에서 방해돌을 제거한다.
 * 호출 시점: 돌이 배치된 후.
 * 통과(2025.10.24)
 */
public final class BlockerBanRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.BLOCKER_BAN,
        1_200
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null || runtime.triggerKind() != RuleTriggerKind.POST_PLACEMENT) {
            return;
        }
        GameSessionStateContext stateContext = runtime.stateContext();
        GameSessionServices services = runtime.services();
        if (stateContext == null || services == null) {
            return;
        }

        GameSessionBoardAccess board = stateContext.board();
        TurnSnapshot snapshot = runtime.turnSnapshot();
        if (snapshot == null) {
            snapshot = services.turnService().snapshot(stateContext.turns());
        }
        StonePlacementMetadata metadata = snapshot != null
            ? StonePlacementMetadata.forRule(snapshot, -1, null)
            : StonePlacementMetadata.systemGenerated();

        int width = board.width();
        int height = board.height();
        boolean mutated = false;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (board.stoneAt(x, y) == Stone.BLOCKER) {
                    services.boardService().setStone(board, x, y, Stone.EMPTY, metadata);
                    mutated = true;
                }
            }
        }

        if (mutated) {
            byte[] snapshotBytes = services.boardService().snapshot(board);
            runtime.contextService()
                .postGame()
                .queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(snapshotBytes, System.currentTimeMillis()));
        }
    }
}
