package teamnova.omok.glue.rule.rules;

import java.util.HashSet;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;

/**
 * 보호 조치: 각 플레이어가 마지막에 둔 돌 주변 한 칸을 금지 구역으로 만든다.
 * 호출 시점: 턴 시작 시(실제로는 직전 차례의 돌이 배치된 직후 적용).
 */
public final class ProtectiveZoneRule implements Rule {
    public static final String STORAGE_KEY = "rule:protectiveZone:data";

    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.PROTECTIVE_ZONE,
        1_800
    );

    private static final int[][] NEIGHBORS = {
        {-1, -1}, {0, -1}, {1, -1},
        {-1, 0}, {1, 0},
        {-1, 1}, {0, 1}, {1, 1}
    };

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
        if (stateContext == null) {
            return;
        }
        TurnPersonalFrame frame = runtime.contextService().turn().currentPersonalTurn(stateContext);
        if (frame == null) {
            return;
        }
        GameSessionBoardAccess board = stateContext.board();
        if (board == null) {
            return;
        }
        ZoneState state = getOrCreateState(access);
        updateZone(state, board, frame.x(), frame.y());
    }

    public static boolean isRestricted(Object data, int x, int y) {
        if (!(data instanceof ZoneState state)) {
            return false;
        }
        return state.restrictedCells.contains(key(x, y));
    }

    private ZoneState getOrCreateState(GameSessionRuleAccess access) {
        Object stored = access.getRuleData(STORAGE_KEY);
        if (stored instanceof ZoneState zoneState) {
            return zoneState;
        }
        ZoneState zoneState = new ZoneState();
        access.putRuleData(STORAGE_KEY, zoneState);
        return zoneState;
    }

    private void updateZone(ZoneState state,
                            GameSessionBoardAccess board,
                            int centerX,
                            int centerY) {
        state.restrictedCells.clear();
        for (int[] delta : NEIGHBORS) {
            int nx = centerX + delta[0];
            int ny = centerY + delta[1];
            if (!board.isWithinBounds(nx, ny)) {
                continue;
            }
            long key = key(nx, ny);
            state.restrictedCells.add(key);
        }
    }

    private static long key(int x, int y) {
        return ((long) x << 32) ^ (long) y;
    }

    private static final class ZoneState {
        private final Set<Long> restrictedCells = new HashSet<>();
    }
}
