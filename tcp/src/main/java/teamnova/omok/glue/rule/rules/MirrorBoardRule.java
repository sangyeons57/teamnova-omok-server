package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;

/**
 * 거울: 플레이어가 둔 돌을 좌우 대칭 위치에도 복제한다.
 * 호출 시점: 돌 배치 후.
 */
public final class MirrorBoardRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.MIRROR_BOARD,
        1_300
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

        TurnPersonalFrame frame = runtime.contextService().turn().currentPersonalTurn(stateContext);
        if (frame == null || !frame.hasActiveMove()) {
            return;
        }
        GameSessionBoardAccess board = stateContext.board();
        int width = board.width();
        int sourceX = frame.x();
        int sourceY = frame.y();
        if (sourceX < 0 || sourceX >= width) {
            return;
        }
        int mirroredX = width - 1 - sourceX;
        if (mirroredX == sourceX) {
            return; // 중앙축 위의 수는 추가 복제하지 않는다.
        }

        Stone placedStone = board.stoneAt(sourceX, sourceY);
        if (placedStone == Stone.EMPTY) {
            return;
        }
        Stone existing = board.stoneAt(mirroredX, sourceY);
        if (existing == placedStone) {
            return;
        }

        StonePlacementMetadata metadata;
        if (frame.currentSnapshot() != null) {
            int actingIndex = stateContext.participants().playerIndexOf(frame.userId());
            metadata = StonePlacementMetadata.forRule(frame.currentSnapshot(), actingIndex, frame.userId());
        } else {
            metadata = StonePlacementMetadata.systemGenerated();
        }

        services.boardService().setStone(board, mirroredX, sourceY, placedStone, metadata);
        byte[] snapshot = services.boardService().snapshot(board);
        runtime.contextService()
            .postGame()
            .queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(snapshot, System.currentTimeMillis()));
    }
}
