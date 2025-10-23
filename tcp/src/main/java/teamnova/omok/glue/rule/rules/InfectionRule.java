package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
 * 감염: 감염 확률과 3턴 후 방해돌 변환을 처리한다.
 * 호출 시점: 전체 턴 종료 시.
 */
public final class InfectionRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.INFECTION,
        1_100
    );

    private static final String DATA_KEY = "rule:infection:active";
    private static final int INFECTION_LIFETIME = 3;
    private static final double BASE_INFECTION_PROBABILITY = 0.2d;
    private static final double ADJACENT_INFECTION_PROBABILITY = 0.45d;

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null || runtime.triggerKind() != RuleTriggerKind.TURN_ROUND_COMPLETED) {
            return;
        }
        GameSessionStateContext stateContext = runtime.stateContext();
        GameSessionServices services = runtime.services();
        if (stateContext == null || services == null) {
            return;
        }
        GameSessionBoardAccess board = stateContext.board();
        Map<Integer, Integer> infections =
            (Map<Integer, Integer>) access.getRuleData(DATA_KEY);
        if (infections == null) {
            infections = new HashMap<>();
            access.putRuleData(DATA_KEY, infections);
        }

        TurnSnapshot snapshot = runtime.turnSnapshot();
        if (snapshot == null) {
            snapshot = services.turnService().snapshot(stateContext.turns());
        }
        boolean mutated = decayAndConvert(board, services, infections, snapshot);
        mutated |= propagateInfections(board, services, infections);

        if (mutated) {
            byte[] bytes = services.boardService().snapshot(board);
            runtime.contextService()
                .postGame()
                .queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(bytes, System.currentTimeMillis()));
        }
    }

    private boolean decayAndConvert(GameSessionBoardAccess board,
                                    GameSessionServices services,
                                    Map<Integer, Integer> infections,
                                    TurnSnapshot snapshot) {
        boolean mutated = false;
        List<Integer> toConvert = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : infections.entrySet()) {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                toConvert.add(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }
        StonePlacementMetadata metadata = buildMetadata(snapshot);
        for (Integer index : toConvert) {
            int width = board.width();
            int x = index % width;
            int y = index / width;
            services.boardService().setStone(board, x, y, Stone.BLOCKER, metadata);
            infections.remove(index);
            mutated = true;
        }
        return mutated;
    }

    private boolean propagateInfections(GameSessionBoardAccess board,
                                        GameSessionServices services,
                                        Map<Integer, Integer> infections) {
        List<int[]> candidates = collectHealthyStones(board, infections);
        if (candidates.isEmpty()) {
            return false;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean mutated = false;

        // Random ambient infection
        if (random.nextDouble() < BASE_INFECTION_PROBABILITY) {
            int[] pick = candidates.get(random.nextInt(candidates.size()));
            int idx = linearIndex(board, pick[0], pick[1]);
            infections.putIfAbsent(idx, INFECTION_LIFETIME);
        }

        // Spread from blockers
        List<int[]> adjacentCandidates = collectAdjacentToBlocker(board, infections);
        for (int[] cell : adjacentCandidates) {
            if (random.nextDouble() < ADJACENT_INFECTION_PROBABILITY) {
                int idx = linearIndex(board, cell[0], cell[1]);
                if (!infections.containsKey(idx)) {
                    infections.put(idx, INFECTION_LIFETIME);
                    mutated = true;
                }
            }
        }
        return mutated;
    }

    private List<int[]> collectHealthyStones(GameSessionBoardAccess board,
                                             Map<Integer, Integer> infections) {
        List<int[]> result = new ArrayList<>();
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Stone stone = board.stoneAt(x, y);
                if (!stone.isPlayerStone()) {
                    continue;
                }
                int idx = linearIndex(board, x, y);
                if (infections.containsKey(idx)) {
                    continue;
                }
                result.add(new int[]{x, y});
            }
        }
        return result;
    }

    private List<int[]> collectAdjacentToBlocker(GameSessionBoardAccess board,
                                                 Map<Integer, Integer> infections) {
        List<int[]> result = new ArrayList<>();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                if (board.stoneAt(x, y) != Stone.BLOCKER) {
                    continue;
                }
                for (int[] dir : dirs) {
                    int nx = x + dir[0];
                    int ny = y + dir[1];
                    if (!board.isWithinBounds(nx, ny)) {
                        continue;
                    }
                    Stone neighbour = board.stoneAt(nx, ny);
                    if (!neighbour.isPlayerStone()) {
                        continue;
                    }
                    int idx = linearIndex(board, nx, ny);
                    if (infections.containsKey(idx)) {
                        continue;
                    }
                    result.add(new int[]{nx, ny});
                }
            }
        }
        return result;
    }

    private StonePlacementMetadata buildMetadata(TurnSnapshot snapshot) {
        if (snapshot != null) {
            return StonePlacementMetadata.forRule(snapshot, -1, null);
        }
        return StonePlacementMetadata.systemGenerated();
    }

    private int linearIndex(GameSessionBoardAccess board, int x, int y) {
        return y * board.width() + x;
    }
}
