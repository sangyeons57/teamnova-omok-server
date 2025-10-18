package teamnova.omok.glue.game.session.services;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.model.BoardStore;
import teamnova.omok.glue.game.session.model.Stone;

/**
 * Stateless utility service for manipulating Omok board data.
 */
public class BoardService implements GameBoardService {
    @Override
    public void reset(BoardStore store) {
        store.clear();
    }

    @Override
    public boolean isWithinBounds(BoardStore store, int x, int y) {
        return x >= 0 && x < store.width() && y >= 0 && y < store.height();
    }

    @Override
    public boolean isEmpty(BoardStore store, int x, int y) {
        return stoneAt(store, x, y) == Stone.EMPTY;
    }

    @Override
    public Stone stoneAt(BoardStore store, int x, int y) {
        return Stone.fromByte(store.get(linearIndex(store, x, y)));
    }

    @Override
    public void setStone(BoardStore store, int x, int y, Stone stone) {
        store.set(linearIndex(store, x, y), stone.code());
    }

    @Override
    public byte[] snapshot(BoardStore store) {
        return store.snapshot();
    }

    @Override
    public boolean hasFiveInARow(BoardStore store, int x, int y, Stone stone) {
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

    private int linearIndex(BoardStore store, int x, int y) {
        return y * store.width() + x;
    }

    private int countDirection(BoardStore store, int startX, int startY, int dx, int dy, Stone playerStone) {
        int count = 0;
        int x = startX + dx;
        int y = startY + dy;
        while (isWithinBounds(store, x, y)) {
            Stone occupying = stoneAt(store, x, y);
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
