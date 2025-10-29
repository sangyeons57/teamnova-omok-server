package teamnova.omok.glue.rule.rules;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 새로운 플레이어: 조커/블로커가 다섯 줄을 만들면 모두 패배.
 * *
 * 통과(2025.10.29)
 */
public final class NewPlayerRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.NEW_PLAYER,
        2_200
    );

    private static final Set<Stone> SPECIAL_STONES = Set.of(Stone.JOKER, Stone.BLOCKER);

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null || runtime.stateContext() == null) {
            return;
        }
        if (runtime.triggerKind() != RuleTriggerKind.OUTCOME_EVALUATION) {
            return;
        }
        GameSessionBoardAccess board = runtime.stateContext().board();
        if (board == null) {
            return;
        }
        if (!hasSpecialFive(board, runtime)) {
            return;
        }
        List<String> participants = runtime.stateContext().participants().getUserIds();
        if (participants == null || participants.isEmpty()) {
            return;
        }
        participants.forEach(userId -> runtime.stateContext().outcomes().updateOutcome(userId, PlayerResult.LOSS));
        System.out.println("[NewPlayerRule] Special stones formed five-in-a-row; all players lose.");
    }

    private boolean hasSpecialFive(GameSessionBoardAccess board, RuleRuntimeContext runtime) {
        var boardService = Objects.requireNonNull(runtime.services().boardService(), "boardService");
        int width = board.width();
        int height = board.height();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Stone stone = board.stoneAt(x, y);
                if (!SPECIAL_STONES.contains(stone)) {
                    continue;
                }
                if (boardService.hasFiveInARowMatching(board, x, y, SPECIAL_STONES)) {
                    return true;
                }
            }
        }
        return false;
    }
}
