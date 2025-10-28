package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.rule.api.BoardSetupRule;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 콜로세움: 가장자리 2칸을 방해돌로 막아 중앙 부분만 사용 가능하게 한다.
 * 호출 시점: 게임 시작 시.
 * 통과(2025.10.28)
 */
public final class ColosseumRule implements Rule, BoardSetupRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.COLOSSEUM,
        600
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // No-op: board setup is performed via the BoardSetupRule capability.
    }

    @Override
    public void setupBoard(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null) {
            return;
        }
        GameSessionBoardAccess board = runtime.stateContext().board();
        GameBoardService boardService = runtime.services().boardService();
        int width = board.width();
        int height = board.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        int margin = Math.min(2, Math.min(width, height) / 2);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean nearEdge = x < margin
                    || y < margin
                    || x >= width - margin
                    || y >= height - margin;
                if (nearEdge) {
                    boardService.setStone(board, x, y, Stone.BLOCKER, StonePlacementMetadata.systemGenerated());
                }
            }
        }
    }
}
