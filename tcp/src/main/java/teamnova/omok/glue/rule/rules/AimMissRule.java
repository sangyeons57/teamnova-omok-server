package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;

/**
 * 에임 미스: 플레이어가 지정한 위치 대신 주변 8칸 중 랜덤한 위치에 돌을 둔다.
 * 호출 시점: 돌이 배치되기 전에.
 *
 * 통과 (2025.10.23)
 */
public final class AimMissRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.AIM_MISS,
        1_000
    );

    private static final int[][] NEIGHBORS = {
        {-1, -1}, {0, -1}, {1, -1},
        {-1, 0}, /* origin */ {1, 0},
        {-1, 1}, {0, 1}, {1, 1}
    };

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (runtime == null || runtime.triggerKind() != RuleTriggerKind.PRE_PLACEMENT) {
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
        int baseX = frame.originalX();
        int baseY = frame.originalY();
        List<int[]> candidates = new ArrayList<>();
        for (int[] offset : NEIGHBORS) {
            int nx = baseX + offset[0];
            int ny = baseY + offset[1];
            if (!board.isWithinBounds(nx, ny)) {
                continue;
            }
            if (board.isEmpty(nx, ny)) {
                candidates.add(new int[]{nx, ny});
            }
        }
        if (candidates.isEmpty()) {
            return; // fallback to the original location already validated
        }
        int[] pick = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        frame.updatePosition(pick[0], pick[1]);
    }
}
