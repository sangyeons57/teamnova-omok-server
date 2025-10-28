package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.runtime.RuleDataKeys;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 진화: 돌 생성 후 10턴이 지나면 자동으로 조커 돌로 변환한다.
 * 호출 시점: 게임 진행 중.
 */
public final class EvolutionRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.EVOLUTION,
        500
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null || runtime.stateContext() == null) {
            return;
        }
        RuleTriggerKind trigger = runtime.triggerKind();
        if (trigger != RuleTriggerKind.POST_PLACEMENT && trigger != RuleTriggerKind.TURN_ADVANCE) {
            return;
        }
        GameSessionStateContext context = runtime.stateContext();
        if (trigger == RuleTriggerKind.TURN_ADVANCE) {
            TurnPersonalFrame frame = context.turnRuntime() != null
                ? context.turnRuntime().currentPersonalTurnFrame()
                : null;
            if (frame != null && frame.hasActiveMove()) {
                // Already processed during POST_PLACEMENT; avoid double aging within the same turn.
                return;
            }
        }
        GameSessionBoardAccess board = context.board();
        GameBoardService boardService = runtime.services().boardService();
        if (board == null || boardService == null) {
            return;
        }
        int width = board.width();
        int height = board.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        int total = width * height;
        int[] ages = (int[]) access.getRuleData(RuleDataKeys.EVOLUTION_AGE_MAP);
        if (ages == null || ages.length != total) {
            ages = new int[total];
        }
        boolean changed = false;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                Stone stone = board.stoneAt(x, y);
                if (stone != null && stone.isPlayerStone()) {
                    ages[index] = Math.min(ages[index] + 1, 10);
                    if (ages[index] >= 10 && stone != Stone.JOKER) {
                        boardService.setStone(board, x, y, Stone.JOKER, StonePlacementMetadata.systemGenerated());
                        ages[index] = 0;
                        changed = true;
                    }
                } else {
                    ages[index] = 0;
                }
            }
        }
        access.putRuleData(RuleDataKeys.EVOLUTION_AGE_MAP, ages);
        if (changed) {
            GameSessionMessenger messenger = runtime.services().messenger();
            if (messenger != null) {
                messenger.broadcastBoardSnapshot(context.session());
            }
        }
    }
}
