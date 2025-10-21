package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;

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
        {-1, 0}, {0, 0}, {1, 0},
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
        TurnCycleContext cycle = runtime.contextService().turn().activeTurnCycle(stateContext);
        if (cycle == null) {
            return;
        }
        GameSessionBoardAccess board = stateContext.board();
        ZoneState state = getOrCreateState(access);
        updateZoneForPlayer(state, board, cycle.userId(), cycle.x(), cycle.y());
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

    private void updateZoneForPlayer(ZoneState state,
                                     GameSessionBoardAccess board,
                                     String userId,
                                     int centerX,
                                     int centerY) {
        if (userId == null) {
            return;
        }
        List<Long> previous = state.byPlayer.remove(userId);
        if (previous != null) {
            for (Long key : previous) {
                state.restrictedCells.remove(key);
                state.ownerByCell.remove(key);
            }
        }
        List<Long> current = new ArrayList<>();
        for (int[] delta : NEIGHBORS) {
            int nx = centerX + delta[0];
            int ny = centerY + delta[1];
            if (!board.isWithinBounds(nx, ny)) {
                continue;
            }
            long key = key(nx, ny);
            current.add(key);
            state.restrictedCells.add(key);
            state.ownerByCell.put(key, userId);
        }
        state.byPlayer.put(userId, current);
    }

    private static long key(int x, int y) {
        return ((long) x << 32) ^ (long) y;
    }

    private static final class ZoneState {
        private final Map<String, List<Long>> byPlayer = new HashMap<>();
        private final Map<Long, String> ownerByCell = new HashMap<>();
        private final Set<Long> restrictedCells = new HashSet<>();
    }
}
