package teamnova.omok.glue.game.session.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.board.BoardPoint;
import teamnova.omok.glue.game.session.model.board.ConnectedGroup;
import teamnova.omok.glue.game.session.model.board.Connectivity;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;

/**
 * Stateless utility service for manipulating Omok board data.
 */
public class BoardService implements GameBoardService {
    @Override
    public void reset(GameSessionBoardAccess store) {
        store.clear();
        store.clearPlacements();
    }

    @Override
    public boolean isWithinBounds(GameSessionBoardAccess store, int x, int y) {
        return store.isWithinBounds(x, y);
    }

    @Override
    public boolean isEmpty(GameSessionBoardAccess store, int x, int y) {
        return store.isEmpty(x, y);
    }

    @Override
    public Stone stoneAt(GameSessionBoardAccess store, int x, int y) {
        return store.stoneAt(x, y);
    }

    @Override
    public void setStone(GameSessionBoardAccess store, int x, int y, Stone stone, StonePlacementMetadata metadata) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(stone, "stone");
        store.setStone(x, y, stone);
        if (stone == Stone.EMPTY) {
            store.clearPlacement(x, y);
        } else if (metadata != null && !metadata.isEmpty()) {
            store.recordPlacement(x, y, metadata);
        } else {
            store.clearPlacement(x, y);
        }
    }

    @Override
    public StonePlacementMetadata placementAt(GameSessionBoardAccess store, int x, int y) {
        return store.placementAt(x, y);
    }

    @Override
    public byte[] snapshot(GameSessionBoardAccess store) {
        return store.snapshot();
    }

    @Override
    public boolean hasFiveInARow(GameSessionBoardAccess store, int x, int y, Stone stone) {
        if (stone == null || stone == Stone.EMPTY || stone.isBlocking()) {
            return false;
        }
        if (!stone.isPlayerStone()) {
            return false;
        }
        return hasFiveInARowMatching(store, x, y, s -> s != null && s.countsForPlayerSequence(stone));
    }

    @Override
    public boolean hasFiveInARowMatching(GameSessionBoardAccess store,
                                         int x,
                                         int y,
                                         Set<Stone> allowedStones) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(allowedStones, "allowedStones");
        if (allowedStones.isEmpty()) {
            return false;
        }
        Stone target = stoneAt(store, x, y);
        if (target == null || !allowedStones.contains(target)) {
            return false;
        }
        return hasFiveInARowMatching(store, x, y, s -> s != null && allowedStones.contains(s));
    }

    private boolean hasFiveInARowMatching(GameSessionBoardAccess store,
                                          int x,
                                          int y,
                                          Predicate<Stone> matcher) {
        int[][] directions = {
            {1, 0},
            {0, 1},
            {1, 1},
            {1, -1}
        };
        for (int[] dir : directions) {
            int count = 1;
            count += countDirection(store, x, y, dir[0], dir[1], matcher);
            count += countDirection(store, x, y, -dir[0], -dir[1], matcher);
            if (count == 5) {
                return true;
            }
        }
        return false;
    }

    private int countDirection(GameSessionBoardAccess store,
                               int startX,
                               int startY,
                               int dx,
                               int dy,
                               Predicate<Stone> matcher) {
        int count = 0;
        int x = startX + dx;
        int y = startY + dy;
        while (isWithinBounds(store, x, y)) {
            Stone occupying = store.stoneAt(x, y);
            if (!matcher.test(occupying)) {
                break;
            }
            count++;
            x += dx;
            y += dy;
        }
        return count;
    }

    @Override
    public List<ConnectedGroup> connectedGroups(GameSessionBoardAccess board, Connectivity connectivity) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(connectivity, "connectivity");
        int width = board.width();
        int height = board.height();
        boolean[] visited = new boolean[width * height];
        int[] queue = new int[width * height];
        List<ConnectedGroup> groups = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                if (visited[idx]) {
                    continue;
                }
                Stone stone = stoneAt(board, x, y);
                if (stone == null || stone == Stone.EMPTY) {
                    visited[idx] = true;
                    continue;
                }
                List<BoardPoint> points = traverseGroup(board, x, y, stone, connectivity, visited, queue);
                groups.add(new ConnectedGroup(stone, points));
            }
        }
        return groups;
    }

    private List<BoardPoint> traverseGroup(GameSessionBoardAccess board,
                                           int startX,
                                           int startY,
                                           Stone target,
                                           Connectivity connectivity,
                                           boolean[] visited,
                                           int[] queue) {
        int width = board.width();
        int height = board.height();
        List<BoardPoint> points = new ArrayList<>();
        int startIndex = startY * width + startX;
        visited[startIndex] = true;
        queue[0] = startIndex;
        int qs = 0;
        int qe = 1;
        while (qs < qe) {
            int index = queue[qs++];
            int x = index % width;
            int y = index / width;
            points.add(new BoardPoint(x, y));
            for (int[] offset : connectivity.offsets()) {
                int nx = x + offset[0];
                int ny = y + offset[1];
                if (!isWithinBounds(board, nx, ny)) {
                    continue;
                }
                int neighborIndex = ny * width + nx;
                if (visited[neighborIndex]) {
                    continue;
                }
                Stone neighbor = stoneAt(board, nx, ny);
                if (neighbor == target) {
                    visited[neighborIndex] = true;
                    queue[qe++] = neighborIndex;
                }
            }
        }
        return points;
    }
}
