package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
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
 * 바둑: 직교 방향으로 자유(빈칸)가 없는 돌은 턴 종료 시 제거한다.
 * 단순화를 위해 연결 그룹 대신 각 돌의 개별 자유만 검사한다.
 * 통과(2025.10.24)
 */
public class GoCaptureRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.GO_CAPTURE,
        1_900
    );

    private static final int[][] DIRECTIONS = new int[][] { {1,0}, {-1,0}, {0,1}, {0,-1} };

    private static final class MutableBoolean {
        boolean value;
        MutableBoolean(boolean value) { this.value = value; }
    }

    private static int handleNeighbor(GameSessionBoardAccess board, int width, int nx, int ny, Stone color, boolean[] visited, int[] queue, int qe, MutableBoolean liberty) {
        Stone ns = board.stoneAt(nx, ny);
        int nearIndex = ny * width + nx;
        if (ns == Stone.EMPTY) {
            liberty.value = true;
        } else if (ns == color && !visited[nearIndex]) {
            visited[nearIndex] = true;
            queue[qe++] = nearIndex;
        }
        //색이 다른 돌이면 아무가능성이 없기때문에 아무 처리도 안함
        return qe;
    }
    private static boolean isInBoard(int width, int height, int pointX, int pointY) {
        return pointX >= 0 && pointX < width && pointY >= 0 && pointY < height;
    }

    @Override
    public RuleMetadata getMetadata() { return METADATA; }

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
        int width = board.width();
        int height = board.height();
        int removed = 0;
        // We'll mark removals in a boolean array to avoid interference during scanning
        boolean[] toRemove = new boolean[width * height];
        boolean[] visited = new boolean[width * height];

        // Group-based liberty check (Go rule): remove only groups with zero liberties
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                if (visited[idx]) continue;

                Stone color = board.stoneAt(x, y);
                if (color.isEmpty()) { // empty
                    visited[idx] = true;
                    continue;
                }

                // BFS to traverse the connected group and detect liberties
                int max = width * height;
                int[] queue = new int[max];
                int[] group = new int[max];
                int queueStart = 0, queueEnd = 0, groupCount = 0;

                // 탐색 시작을 표시하기 위해 작성
                queue[queueEnd++] = idx;
                visited[idx] = true;
                boolean groupHasLiberty = false;

                while (queueStart < queueEnd) {
                    int cur = queue[queueStart++];
                    int cx = cur % width;
                    int cy = cur / width;
                    group[groupCount++] = cur;

                    MutableBoolean liberty = new MutableBoolean(groupHasLiberty);
                    for (int[] direction : DIRECTIONS) {
                        int nearX = cx + direction[0];
                        int nearY = cy + direction[1];
                        if (isInBoard(width, height, nearX, nearY)) {
                            queueEnd = handleNeighbor(board, width, nearX, nearY, color, visited, queue, queueEnd, liberty);
                        }
                    }
                    groupHasLiberty = liberty.value;
                }

                if (!groupHasLiberty) {
                    for (int i = 0; i < groupCount; i++) {
                        toRemove[group[i]] = true;
                    }
                }
            }
        }
        for (int i = 0; i < toRemove.length; i++) {
            if (toRemove[i]) {
                int x = i % width; int y = i / width;
                services.boardService().setStone(board, x, y, Stone.EMPTY, null);
                removed++;
            }
        }
        if (removed > 0) {
            System.out.println("[RULE_LOG] GoCaptureRule removed " + removed + " stones (no liberties)");
            byte[] boardSnapshot = services.boardService().snapshot(board);
            runtime.contextService().postGame().queueBoardSnapshot(stateContext, new BoardSnapshotUpdate(boardSnapshot, System.currentTimeMillis()));
        }
    }
}
