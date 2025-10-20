package teamnova.omok.glue.game.session.services;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.model.Stone;
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
        int[][] directions = {
            {1, 0}, // horizontal
            {0, 1}, // vertical
            {1, 1}, // diagonal down-right
            {1, -1} // diagonal up-right
        };
        for (int[] dir : directions) {
            int count = 1;
            count += countDirection(store, x, y, dir[0], dir[1], stone);
            count += countDirection(store, x, y, -dir[0], -dir[1], stone);
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }

    private int countDirection(GameSessionBoardAccess store, int startX, int startY, int dx, int dy, Stone playerStone) {
        int count = 0;
        int x = startX + dx;
        int y = startY + dy;
        while (isWithinBounds(store, x, y)) {
            Stone occupying = store.stoneAt(x, y);
            if (!occupying.countsForPlayerSequence(playerStone)) {
                break;
            }
            count++;
            x += dx;
            y += dy;
        }
        return count;
    }
}
